package me.hex539.contest;

import static com.google.common.truth.Truth.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.protobuf.util.Durations;

import me.hex539.contest.ResolverController.Observer;
import me.hex539.contest.ResolverController.Resolution;

import edu.clics.proto.ClicsProto.*;

import org.junit.Test;

public class ResolverTest {
  @Test
  public void testResolveNwerc2017() throws Exception {
    final ClicsContest entireContest =
        new ContestDownloader(getClass().getResourceAsStream("/resources/contests/nwerc2017.pb")).fetch();

    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> "12890".equals(g.getId()))
            .filterTooLateSubmissions()
            .build();

    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference, false)
        .addObserver(model);

    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, never()).onProblemSubmitted(any(), any());
    verify(observer, times(400)).onProblemScoreChanged(any(), any());
    verify(observer, times(53)).onScoreChanged(any(), any());
    verify(observer, times(53)).onTeamRankChanged(any(), anyInt(), anyInt());
    verify(observer, times(120)).onTeamRankFinalised(any(), anyInt());

    assertThat(model.getRows().size()).isEqualTo(reference.getRows().size());
    for (int i = 0; i < 120; i++) {
      assertThat(model.getRow(i)).isEqualTo(reference.getRow(i));
    }
  }

  @Test
  public void testResolveNwerc2018() throws Exception {
    final ClicsContest entireContest =
        new ContestDownloader(getClass().getResourceAsStream("/resources/contests/nwerc2018.pb")).fetch();

    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> !g.getHidden())
            .filterTooLateSubmissions()
            .build();

    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference, false)
        .addObserver(model);

    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, never()).onProblemSubmitted(any(), any());
    verify(observer, times(413)).onProblemScoreChanged(any(), any());
    verify(observer, times(69)).onScoreChanged(any(), any());
    verify(observer, times(68)).onTeamRankChanged(any(), anyInt(), anyInt());
    verify(observer, times(122)).onTeamRankFinalised(any(), anyInt());

    assertThat(model.getRows().size()).isEqualTo(reference.getRows().size());
    for (int i = 0; i < 122; i++) {
      assertThat(model.getRow(i)).isEqualTo(reference.getRow(i));
    }
  }

  @Test
  public void testResolveLargeNumberOfTeams() throws Exception {
    final int n = 10_000;

    final ClicsContest.Builder ccBuilder = ClicsContest.newBuilder()
        .setContest(Contest.newBuilder()
            .setContestDuration(Durations.fromMillis(TimeUnit.HOURS.toMillis(2)))
            .setScoreboardFreezeDuration(Durations.fromMillis(TimeUnit.HOURS.toMillis(1)))
            .setPenaltyTime(20)
            .build())
        .putJudgementTypes("AC", JudgementType.newBuilder().setId("AC").setSolved(true).build())
        .putProblems("A", Problem.newBuilder().setId("A").setLabel("A").setOrdinal(0).build())
        .putProblems("B", Problem.newBuilder().setId("B").setLabel("B").setOrdinal(1).build())
        .putGroups("P", Group.newBuilder().setId("P").setName("Participants").build());
    for (int i = 0; i < n; i++) {
      final String teamId = String.format("t%08d", i);
      ccBuilder.putTeams(teamId, Team.newBuilder().setId(teamId).setName(teamId).addGroupIds("P").build());
      ccBuilder.putSubmissions(teamId + "s1", Submission.newBuilder()
          .setId(teamId + "s1")
          .setProblemId("A")
          .setTeamId(teamId)
          .setContestTime(Durations.fromMillis(TimeUnit.MINUTES.toMillis(100)))
          .build());
      ccBuilder.putSubmissions(teamId + "s2", Submission.newBuilder()
          .setId(teamId + "s2")
          .setProblemId("B")
          .setTeamId(teamId)
          .setContestTime(Durations.fromMillis(TimeUnit.MINUTES.toMillis(100)))
          .build());
      ccBuilder.addScoreboard(ScoreboardRow.newBuilder()
          .setRank(i + 1)
          .setTeamId(teamId)
          .setScore(ScoreboardScore.newBuilder().setNumSolved(2).setTotalTime(200))
          .addProblems(ScoreboardProblem.newBuilder()
              .setProblemId("A").setNumJudged(1).setSolved(true).setTime(100).build())
          .addProblems(ScoreboardProblem.newBuilder()
              .setProblemId("B").setNumJudged(1).setSolved(true).setTime(100).build())
          .build());
    }
    final ClicsContest entireContest = ccBuilder.build();
    final ScoreboardModel reference = ScoreboardModelImpl.newBuilder(entireContest).build();
    assertThat(reference.getTeams().size()).isEqualTo(n);
    
    ResolverController resolver = new ResolverController(entireContest, reference);
    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, times(2 * n)).onTeamRankChanged(any(), anyInt(), eq(1));
  }
}
