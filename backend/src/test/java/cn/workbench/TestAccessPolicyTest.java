package cn.workbench;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;

class TestAccessPolicyTest {
 private final Clock clock=Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"),ZoneOffset.UTC);
 @Test void bypassRequiresExplicitTestProfileAllowlistAndExpiry(){
  var allowed=new TestAccessPolicy(true,"41,42","2026-11-07T00:00:00Z",clock);
  assertThat(allowed.allows(41)).isTrue();
  assertThat(allowed.allows(99)).isFalse();
  assertThat(new TestAccessPolicy(false,"41","2026-11-07T00:00:00Z",clock).allows(41)).isFalse();
  assertThat(new TestAccessPolicy(true,"41","2026-11-05T00:00:00Z",clock).allows(41)).isFalse();
  assertThat(new TestAccessPolicy(true,"41","invalid",clock).allows(41)).isFalse();
  assertThat(new TestAccessPolicy(true,"41","2026-12-01T00:00:00Z",clock).allows(41)).isFalse();
 }
}
