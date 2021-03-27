package git.utils

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.control.Breaks._

object LeetCode {

  def lengthOfLongestSubstring(s: String): Int = {
    var charExist: Set[Char] = Set[Char]()
    var max: Int = 0

    for ((char, index) <- s.zipWithIndex) {
      var current = char
      var currentIndex = index
      var stop = false
      while (currentIndex < s.length && !stop)
        if (!charExist.contains(current)) {
          charExist += current
          currentIndex = if (currentIndex == s.length - 1) currentIndex else currentIndex + 1
          current = s(currentIndex)
        } else {
          stop = true
          max = if (max > (currentIndex - index + 1)) max else currentIndex - index + 1
          charExist = Set[Char]()
        }
    }

    max = if (max > charExist.size) max else charExist.size
    max
  }

  def isPalindrome(s: String): Boolean = {
    @tailrec
    def isPalindromeHelper(seq: Seq[Char]): Boolean =
      seq match {
        case Nil => true
        case Seq(single) =>
          println(single)
          true
        case Seq(head, tail @ _*) => head == tail.last && isPalindromeHelper(tail.dropRight(1))
        // case head +: tail => head == tail.last && isPalindromeHelper(tail.dropRight(1))
      }

    isPalindromeHelper(s)
  }
}
