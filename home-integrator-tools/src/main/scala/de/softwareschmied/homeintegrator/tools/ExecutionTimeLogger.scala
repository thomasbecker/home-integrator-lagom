package de.softwareschmied.homeintegrator.tools

import org.slf4j.LoggerFactory

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 2019-03-22.
  */
trait ExecutionTimeLogger {
  private val log = LoggerFactory.getLogger(classOf[ExecutionTimeLogger])

  def time[R](f: => R) = {
    val start = System.nanoTime()
    val result = f
    val end = System.nanoTime()
    log.info(s"Time taken for ${(end - start) * 1000}ms")
    result
  }
}
