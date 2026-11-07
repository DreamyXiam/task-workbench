package cn.workbench;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Explicit, time-limited exception for operator-provisioned test accounts only. */
@Component
public class TestAccessPolicy {
 private final boolean enabled;
 private final Set<Long> accountIds;
 private final Instant expires;
 private final Clock clock;

 @Autowired
 public TestAccessPolicy(Environment environment,
   @Value("${TASK_TEST_NO_2FA_IDS:}") String ids,
   @Value("${TASK_TEST_NO_2FA_EXPIRES_AT:}") String until){
  this(Arrays.asList(environment.getActiveProfiles()).contains("test-access"),ids,until,Clock.systemUTC());
 }

 TestAccessPolicy(boolean profile,String ids,String until,Clock clock){
  this.clock=clock;
  Set<Long> parsed=Set.of();Instant cutoff=Instant.EPOCH;boolean valid=false;
  try{
   parsed=Arrays.stream(ids.split(",")).map(String::trim).map(Long::parseLong).filter(id->id>0).collect(Collectors.toUnmodifiableSet());
   cutoff=Instant.parse(until);
   valid=profile&&!parsed.isEmpty()&&parsed.size()<=12&&cutoff.isAfter(clock.instant())&&!cutoff.isAfter(clock.instant().plus(Duration.ofDays(7)));
  }catch(RuntimeException ignored){/* Invalid configuration fails closed. */}
  this.enabled=valid;this.accountIds=parsed;this.expires=cutoff;
 }
 public boolean allows(long userId){return enabled&&clock.instant().isBefore(expires)&&accountIds.contains(userId);}
}
