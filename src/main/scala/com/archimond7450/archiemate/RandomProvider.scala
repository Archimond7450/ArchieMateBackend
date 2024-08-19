package com.archimond7450.archiemate

import scala.util.Random

class RandomProvider {
  private val random = new Random

  def nextLong(from: Long, to: Long): Long = random.nextLong(to - from) - from
}
