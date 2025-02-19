package me.hex539.resolver;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;
import me.hex539.resolver.input.Gamepad;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

public class ResolverWindow extends Thread {
  private static final boolean PRINT_FPS = false;
  private static final boolean LIMIT_FPS = true;
  private static final long MAX_FPS = 60;

  private final ResolverController resolver;
  private final ScoreboardModel model;
  private final Renderer renderer;
  private final Controller controller;

  private final Semaphore exit = new Semaphore(0);
  private final Semaphore advance = new Semaphore(0);
  private final Semaphore toggleFullscreen = new Semaphore(0);
  private final Semaphore resizeWindow = new Semaphore(0);

  int windowX = -1;
  int windowY = -1;
  int windowHeight = -1;
  int windowWidth = -1;
  boolean isFullscreen = false;

  public ResolverWindow(
      CompletableFuture<? extends ResolverController> resolver,
      CompletableFuture<? extends ScoreboardModel> model,
      CompletableFuture<? extends ByteBuffer> ttfData) throws Exception {
    this.model = model.get();
    this.resolver = resolver.get();
    this.controller = new Controller(this.resolver);
    this.renderer = new Renderer(this.model, ttfData);

    this.resolver.addObserver(this.renderer);
  }

  public void run() {
    if (!glfwInit()) {
      System.exit(1);
    }
    GLFWErrorCallback.createPrint().set();

    long primaryMonitor = glfwGetPrimaryMonitor();
    GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);

    windowHeight = Math.max(videoMode.height() * 3 / 4, 1);
    windowWidth = Math.min(videoMode.width(), windowHeight * 16 / 9);

    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    final long window = glfwCreateWindow(
        windowWidth,
        windowHeight,
        "Resolver",
        /* display= */ NULL,
        NULL);
    if (window == 0L) {
      System.err.println("Failed to create a GLFW window for OpenGL.");
      System.exit(1);
    }
    glfwMakeContextCurrent(window);
    createCapabilities();

    final Set<Gamepad> gamepads = Gamepad.findAll();
    glfwSetKeyCallback(window, this::onKeyCallback);
    glfwSetMouseButtonCallback(window, this::onMouseButtonCallback);
    glfwSetScrollCallback(window, this::onScrollCallback);
    glfwSetWindowPosCallback(window, this::onWindowPosCallback);
    glfwSetWindowSizeCallback(window, this::onWindowSizeCallback);
    renderer.setVideoMode(windowWidth, windowHeight);

    ArrayDeque<Long> frames = PRINT_FPS ? new ArrayDeque<>() : null;
    final long minFrameTime = LIMIT_FPS ? TimeUnit.SECONDS.toNanos(1) / MAX_FPS : 0L;

    for (long lastFrameTime = 0; !glfwWindowShouldClose(window) && !exit.tryAcquire();) {
      if (toggleFullscreen.tryAcquire()) {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
          primaryMonitor = getGlfwWindowMonitor(window).orElse(primaryMonitor);
          videoMode = glfwGetVideoMode(primaryMonitor);

          glfwSetWindowMonitor(
              window,
              primaryMonitor,
              0, 0, videoMode.width(), videoMode.height(),
              videoMode.refreshRate());
          renderer.setVideoMode(videoMode.width(), videoMode.height());
        } else {
          resizeWindow.release();
        }
      }
      if (resizeWindow.tryAcquire()) {
        glfwSetWindowMonitor(
            window,
            NULL,
            windowX, windowY, windowWidth, windowHeight,
            GLFW_DONT_CARE);
        renderer.setVideoMode(windowWidth, windowHeight);
      }

      long timeNow = System.nanoTime();
      if (LIMIT_FPS) {
        timeNow += (long) (1e9 / MAX_FPS);
      }

      if (PRINT_FPS) {
        frames.add(timeNow);
        while (frames.peekFirst() <= timeNow - TimeUnit.SECONDS.toNanos(1)) {
          frames.pollFirst();
        }
        System.err.println("FPS: " + frames.size());
      }

      updateJoysticks(gamepads, (timeNow - lastFrameTime) / 1e9);

      boolean active = false;
      glClear(GL_COLOR_BUFFER_BIT);
      active |= renderer.mainLoop(timeNow);
      active |= controller.mainLoop(timeNow);
      glfwSwapBuffers(window);

      if (LIMIT_FPS) {
        long sleepDuration = (lastFrameTime + minFrameTime) - timeNow;
        if (sleepDuration > 0) {
          try {
            Thread.sleep(sleepDuration / 1000000, (int) (sleepDuration % 1000000));
          } catch (InterruptedException e) {
          }
          timeNow = lastFrameTime + minFrameTime;
        }
      }
      lastFrameTime = timeNow;

      active = true;
      if (active) {
        glfwPollEvents();
      } else {
        glfwWaitEvents();
      }
    }
    glfwTerminate();
  }

  /**
   * Find which monitor {@param win} is most likely associated with for the
   * purpose of switching in and out of fullscreen.
   */
  private Optional<Long> getGlfwWindowMonitor(long win) {
    try (MemoryStack s = stackPush()) {
      IntBuffer x = s.mallocInt(1);
      IntBuffer y = s.mallocInt(1);
      IntBuffer w = s.mallocInt(1);
      IntBuffer h = s.mallocInt(1);
      PointerBuffer monitors = glfwGetMonitors();
      long bestArea = 0;
      long bestMonitor = -1;
      for (int i = 0; i < monitors.remaining(); i++) {
        final long monitor = monitors.get(i);
        glfwGetMonitorWorkarea(monitor, x, y, w, h);
        final long minX = Math.max(x.get(0), windowX);
        final long minY = Math.max(y.get(0), windowY);
        final long maxX = Math.min(x.get(0) + w.get(0), windowX + windowWidth);
        final long maxY = Math.min(y.get(0) + h.get(0), windowY + windowHeight);
        if (minX < maxX && minY < maxY) {
          final long area = (maxX - minX) * (maxY - minY);
          if (area > bestArea) {
            bestArea = area;
            bestMonitor = monitor;
          }
        }
      }
      if (bestMonitor != -1) {
        return Optional.ofNullable(bestMonitor);
      }
    }
    return Optional.empty();
  }

  private void onWindowPosCallback(long win, int x, int y) {
    if (isFullscreen) {
      return;
    }
    windowX = x;
    windowY = y;
  }

  private void onWindowSizeCallback(long win, int width, int height) {
    if (isFullscreen) {
      return;
    }
    windowWidth = width;
    windowHeight = height;
    resizeWindow.release();
  }

  private void onMouseButtonCallback(long win, int button, int action, int mods) {
    if (action == GLFW_PRESS) {
      controller.onAdvance();
    }
  }

  private void onScrollCallback(long win, double xOffset, double yOffset) {
    renderer.onScroll(yOffset);
  }

  private void updateJoysticks(Set<? extends Gamepad> gamepads, double duration) {
    double dist = 0.0;
    for (Gamepad gamepad : gamepads) {
      if (gamepad.update()) {
        dist += gamepad.getScroll();
        for (int i = gamepad.getPresses(); i --> 0;) {
          controller.onAdvance();
        }
      }
    }
    if (dist != 0) {
      renderer.onScroll(-20.0 * duration * dist, /* smooth= */ false);
    }
  }

  private void onKeyCallback(long win, int key, int scancode, int action, int mods) {
    if ((mods & GLFW_MOD_ALT) != 0) {
      if (action == GLFW_PRESS) {
        onAltKeyCallback(win, key, scancode, action);
      }
    } else {
      if (action == GLFW_RELEASE || action == GLFW_REPEAT) {
        onKeyCallback(win, key, scancode, action);
      }
    }
  }

  private void onAltKeyCallback(long win, int key, int scancode, int action) {
    switch (key) {
      case GLFW_KEY_ENTER: {
        toggleFullscreen.release();
        return;
      }
      default: {
        return;
      }
    }
  }

  private void onKeyCallback(long win, int key, int scancode, int action) {
    switch (key) {
      case GLFW_KEY_ESCAPE: {
        exit.release();
        return;
      }
      case GLFW_KEY_F11:
      case 'F': {
        toggleFullscreen.release();
        return;
      }
      case GLFW_KEY_ENTER:
      case GLFW_KEY_SPACE:
      case GLFW_KEY_UP:
      case GLFW_KEY_RIGHT:
      case GLFW_KEY_LEFT:
      case GLFW_KEY_DOWN: {
        controller.onAdvance();
        return;
      }
      default: {
        return;
      }
    }
  }
}
