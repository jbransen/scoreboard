package me.hex539.resolver;

import com.google.auto.value.AutoValue;

import java.io.*;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.BufferUtils;

import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.*;

public class FontRenderer {

  private static final int WIDTH = 512;
  private static final int HEIGHT = 512;
  private static final int FONT_HEIGHT = 24;

  private final ByteBuffer ttfData;

  private final STBTTFontinfo fontInfo;
  private int ascent;
  private int descent;
  private int lineGap;

  private STBTTBakedChar.Buffer cdata = null;
  private int texId = 0;

  private double screenWidth = 1;
  private double screenHeight = 1;

  public FontRenderer() {
    ttfData = mapResource("/resources/fonts/FiraSans-Regular.ttf");

    fontInfo = STBTTFontinfo.create();
    if (!stbtt_InitFont(fontInfo, ttfData)) {
      throw new IllegalStateException("Failed to initialize font information.");
    }

    try (MemoryStack stack = stackPush()) {
      final IntBuffer pAscent  = stack.mallocInt(1);
      final IntBuffer pDescent = stack.mallocInt(1);
      final IntBuffer pLineGap = stack.mallocInt(1);
      stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap);
      this.ascent = pAscent.get(0);
      this.descent = pDescent.get(0);
      this.lineGap = pLineGap.get(0);
    }
  }

  private void initGraphics() {
    texId = glGenTextures();
    cdata = STBTTBakedChar.malloc(128500);

    ByteBuffer bitmap = BufferUtils.createByteBuffer(WIDTH * HEIGHT);
    stbtt_BakeFontBitmap(ttfData, FONT_HEIGHT, bitmap, WIDTH, HEIGHT, 32, cdata);

    glBindTexture(GL_TEXTURE_2D, texId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, WIDTH, HEIGHT, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

  }

  public void destroyGraphics() {
    if (cdata != null) {
      // glDeleteTextures(1, &texId);
      cdata = null;
    }
  }

  public void setVideoSize(double width, double height) {
    screenWidth = width;
    screenHeight = height;
  }

  public void drawText(double x, double y, String text) {
    if (cdata == null) {
      initGraphics();
    }

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    renderText(x, y, text);

    glDisable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
  }

  private void renderText(double startX, double startY, String text) {
    float scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);

    try (MemoryStack stack = stackPush()) {
      IntBuffer pCodePoint = stack.mallocInt(1);

      FloatBuffer x = stack.floats((float) startX);
      FloatBuffer y = stack.floats((float) startY);

      STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);

      int lineStart = 0;

      float factorX = 1.0f ;/// getContentScaleX();
      float factorY = -1.0f ;/// getContentScaleY();
      float lineY = (float) startY;

      glColor3d(1.0, 1.0, 1.0);

      glBegin(GL_QUADS);
      for (int i = 0, to = text.length(); i < to; ) {
        i += getCP(text, to, i, pCodePoint);

        int cp = pCodePoint.get(0);
        if (cp == '\n') {
          y.put(0, lineY = y.get(0) + (ascent - descent + lineGap) * scale);
          x.put(0, 0);

          lineStart = i;
          continue;
//        } else if (cp < 32 || 128 <= cp) {
//          continue;
        }

        float cpX = x.get(0);
        stbtt_GetBakedQuad(cdata, WIDTH, HEIGHT, cp - 32, x, y, q, true);
        x.put(0, scale(cpX, x.get(0), factorX));
        if (i < to) {
          getCP(text, to, i, pCodePoint);
          x.put(0, x.get(0) + stbtt_GetCodepointKernAdvance(fontInfo, cp, pCodePoint.get(0)) * scale);
        }

        float
          x0 = scale(cpX, q.x0(), factorX),
          x1 = scale(cpX, q.x1(), factorX),
          y0 = scale(lineY, q.y0(), factorY),
          y1 = scale(lineY, q.y1(), factorY);

        glTexCoord2f(q.s0(), q.t0());
        glVertex2f(x0, y0);

        glTexCoord2f(q.s1(), q.t0());
        glVertex2f(x1, y0);

        glTexCoord2f(q.s1(), q.t1());
        glVertex2f(x1, y1);

        glTexCoord2f(q.s0(), q.t1());
        glVertex2f(x0, y1);
      }
      glEnd();
    }
  }

  private static float scale(float centre, float offset, float factor) {
    return (offset - centre) * factor + centre;
  }

  private static int getCP(String text, int to, int i, IntBuffer cpOut) {
    char c1 = text.charAt(i);
    if (Character.isHighSurrogate(c1) && i + 1 < to) {
      char c2 = text.charAt(i + 1);
      if (Character.isLowSurrogate(c2)) {
        cpOut.put(0, Character.toCodePoint(c1, c2));
        return 2;
      }
    }
    cpOut.put(0, c1);
    return 1;
  }


  private ByteBuffer mapResource(String location) {
    try (final InputStream is = getClass().getResourceAsStream(location)) {
      if (is == null) {
        throw new Error("Resource does not exist: " + location);
      }
      final byte[] data = is.readAllBytes();
      final ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
      buffer.put(data, 0, data.length);
      buffer.flip();
      return buffer;
    } catch (IOException e) {
      throw new Error("Failed to map resource: " + location, e);
    }
  }
}
