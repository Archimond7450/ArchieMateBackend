package com.archimond7450.archiemate.twitch.irc

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BadgeInfoDecoderSpec extends AnyFunSpec with Matchers {

  describe("Twitch examples") {
    it("Prediction with space in value") {
      val decoder = new BadgeInfoDecoder
      val badgeInfo = "predictions/wtiiPig Yes,subscriber/8"
      val expected = Map("predictions" -> "wtiiPig Yes", "subscriber" -> "8")
      decoder.decode(badgeInfo) shouldBe expected
    }
  }
}
