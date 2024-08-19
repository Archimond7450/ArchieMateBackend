package com.archimond7450.archiemate.twitch.irc

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TagsDecoderSpec extends AnyFunSpec with Matchers {
  val tagsDecoder = new TagsDecoder

  describe("decode method") {
    describe("returns None") {
      it("when empty string is provided") {
        tagsDecoder.decode("") shouldBe None
      }

      it("when the '@' character is missing at the start") {
        val invalidTags = "room-id=1234;user-id=6789"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldBe expected
      }

      it("when there is a semicolon in a tag's value") {
        val invalidTags = "@display-name=Bad;Name;user-id=666"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldBe expected
      }

      describe("when there is no '=' character in one of the tags") {
        it("invalid first tag") {
          val invalidTags = "@login;tmi-sent-ts=1234567"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid second tag") {
          val invalidTags = "@room-id=2222;tmi-sent-ts"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid third tag") {
          val invalidTags = "@ban-duration=1;room-id=4321;target-user-id;tmi-sent-ts=7654321"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }
      }

      describe("when there is extra '=' character in one of the tags") {
        it("invalid first tag") {
          val invalidTags = "@msg-param-sub-plan-name=Sub\\s=\\sgood;msg-param-sub-plan=1000"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid second tag") {
          val invalidTags = "@msg-param-should-share-streak=0;msg-param-sub-plan-name==Sub=;msg-param-sub-plan=3000"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid third tag") {
          val invalidTags = "@msg-param-should-share-streak=0;msg-param-streak-months=0;msg-param-sub-plan-name=Sub2=better;msg-param-sub-plan=2000"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }
      }

      describe("when there is a space in one of the tags") {
        it("invalid first tag") {
          val invalidTags = "@msg-param-sub-plan-name=Good Sub;msg-id=sub;msg-param-sub-plan=1000"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid second tag") {
          val invalidTags = "msg-id=sub;msg-param-sub-plan-name=Sub Plan Name;msg-param-sender-login=user"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }

        it("invalid third tag") {
          val invalidTags = "msg-id=sub;msg-param-sub-plan=3000;msg-param-sub-plan-name=The Best Sub Plan Ever!"
          val expected = None

          tagsDecoder.decode(invalidTags) shouldBe expected
        }
      }
    }

    describe("returns Some") {
      it("one correct tag provided") {
        val tags = "@msg-id=delete_message_success"
        val expected = Some(Map("msg-id" -> "delete_message_success"))

        tagsDecoder.decode(tags) shouldBe expected
      }

      it("two correct tags provided") {
        val tags = "@msg-id=sub;msg-param-sub-plan=Prime"
        val expected = Some(
          Map(
            "msg-id" -> "sub",
            "msg-param-sub-plan" -> "Prime"
          )
        )

        tagsDecoder.decode(tags) shouldBe expected
      }

      it("three correct tags provided") {
        val tags = "@room-id=1234;target-user-id=8765;tmi-sent-ts=1642715756806"
        val expected = Some(
          Map(
            "room-id" -> "1234",
            "target-user-id" -> "8765",
            "tmi-sent-ts" -> "1642715756806"
          )
        )

        tagsDecoder.decode(tags) shouldBe expected
      }
    }
  }

  describe("unescapeValue method") {
    it("returns the same value when there is nothing to escape") {
      val value = "TestChannel123"
      val expected = value

      tagsDecoder.unescapeValue(value) shouldBe expected
    }

    it("returns empty string when empty string is passed") {
      val value = ""
      val expected = value

      tagsDecoder.unescapeValue(value) shouldBe expected
    }

    it("returns correct unescaped value for test\\") {
      val value = "test\\"
      val expected = "test"

      tagsDecoder.unescapeValue(value) shouldBe expected
    }

    it("returns correct unescaped value for \\b") {
      val value = "\\b"
      val expected = "b"

      tagsDecoder.unescapeValue(value) shouldBe expected
    }

    it("returns correct unescaped value for \\\\") {
      val value = "\\\\"
      val expected = "\\"

      tagsDecoder.unescapeValue(value) shouldBe expected
    }

    it("returns correct unescaped value for bigger example") {
      val value = "This\\sis\\sa\\stest\\:\\salso\\sthis\\r\\nAnother\\sline\\sof\\stext:\\s\\\\\\\\localhost\\\\someFile.txt"
      val expected = "This is a test; also this\r\nAnother line of text: \\\\localhost\\someFile.txt"

      tagsDecoder.unescapeValue(value) shouldBe expected
    }
  }
}
