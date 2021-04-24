/**
 * Copyright (c) 2020, the Alpha Team.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.core.externals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.externals.Predicate;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * Collection of methods that can be used as external atoms from ASP programs.
 * Provides commonly used functionality such as basic string operations,
 * datetime handling etc.
 * 
 * All functions exposed by this class are guaranteed to be stateless and
 * side-effect free.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public final class AspStandardLibrary {

	private AspStandardLibrary() {
		throw new AssertionError(this.getClass().getSimpleName() + " is a non-instantiable utility class!");
	}

	/**
	 * Parses a string representing a datetime without time-zone and returns the
	 * year, month, day, hours, minutes and seconds as separate symbolic integer
	 * terms.
	 * Example:
	 * 
	 * <pre>
	 * 		A valid ground instance of the atom &stdlib_datetime_parse[DTSTR, "dd.mm.yyyy hh:MM:ss"](YEAR, MONTH, DAY, HOUR, MIN, SEC)
	 * 		would be: &stdlib_datetime_parse["20.05.2020 01:19:13", "dd.mm.yyyy hh:MM:ss"](2020, 5, 20, 1, 19, 13)
	 * </pre>
	 * 
	 * Timezones are not supported by this function. Datetime values are parsed
	 * using {@link LocalDateTime.parse}.
	 * 
	 * @param datetime a string representing a datetime without time zone
	 *                 information
	 * @param format   a format string that is accepted by {@link DateTimeFormatter}
	 * @return a 6-value integer tuple of format (YEAR, MONTH, DAY, HOUR, MIN, SEC)
	 */
	@Predicate(name = "stdlib_datetime_parse")
	public static Set<List<ConstantTerm<Integer>>> datetimeParse(String dtstr, String format) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		LocalDateTime datetime = LocalDateTime.parse(dtstr, formatter);
		List<ConstantTerm<Integer>> terms = Terms.asTermList(
				datetime.getYear(), datetime.getMonth().getValue(), datetime.getDayOfMonth(),
				datetime.getHour(), datetime.getMinute(), datetime.getSecond());
		return Collections.singleton(terms);
	}

	/**
	 * Compares two datetimes and returns true iff the first datetime (dt1) is
	 * before the second datetime (dt2). Both datetimes are represented as six
	 * integers each, referring to years, months, days, hours, minutes and seconds
	 * respectively.
	 * 
	 * @param dt1Year   the year field for dt1
	 * @param dt1Month  the month field for dt1
	 * @param dt1Day    the day field for dt1
	 * @param dt1Hour   the hour field for dt1
	 * @param dt1Minute the minute field for dt1
	 * @param dt1Second the second field for dt1
	 * @param dt2Year   the year field for dt2
	 * @param dt2Month  the month field for dt2
	 * @param dt2Day    the day field for dt2
	 * @param dt2Hour   the hour field for dt2
	 * @param dt2Minute the minute field for dt2
	 * @param dt2Second the second field for dt2
	 * @return true if dt1 is before dt2 in time, false otherwise
	 */
	@Predicate(name = "stdlib_datetime_is_before")
	public static boolean datetimeIsBefore(int dt1Year, int dt1Month, int dt1Day, int dt1Hour, int dt1Minute, int dt1Second,
			int dt2Year, int dt2Month, int dt2Day, int dt2Hour, int dt2Minute, int dt2Second) {
		LocalDateTime dt1 = LocalDateTime.of(dt1Year, dt1Month, dt1Day, dt1Hour, dt1Minute, dt1Second);
		LocalDateTime dt2 = LocalDateTime.of(dt2Year, dt2Month, dt2Day, dt2Hour, dt2Minute, dt2Second);
		return dt1.isBefore(dt2);
	}

	/**
	 * Compares two datetimes and returns true iff the first datetime (dt1) is
	 * equal to the second datetime (dt2). Both datetimes are represented as six
	 * integers each, referring to years, months, days, hours, minutes and seconds
	 * respectively.
	 * 
	 * @param dt1Year   the year field for dt1
	 * @param dt1Month  the month field for dt1
	 * @param dt1Day    the day field for dt1
	 * @param dt1Hour   the hour field for dt1
	 * @param dt1Minute the minute field for dt1
	 * @param dt1Second the second field for dt1
	 * @param dt2Year   the year field for dt2
	 * @param dt2Month  the month field for dt2
	 * @param dt2Day    the day field for dt2
	 * @param dt2Hour   the hour field for dt2
	 * @param dt2Minute the minute field for dt2
	 * @param dt2Second the second field for dt2
	 * @return true if dt1 is equal to dt2, false otherwise
	 */
	@Predicate(name = "stdlib_datetime_is_equal")
	public static boolean datetimeIsEqual(int dt1Year, int dt1Month, int dt1Day, int dt1Hour, int dt1Minute, int dt1Second,
			int dt2Year, int dt2Month, int dt2Day, int dt2Hour, int dt2Minute, int dt2Second) {
		LocalDateTime dt1 = LocalDateTime.of(dt1Year, dt1Month, dt1Day, dt1Hour, dt1Minute, dt1Second);
		LocalDateTime dt2 = LocalDateTime.of(dt2Year, dt2Month, dt2Day, dt2Hour, dt2Minute, dt2Second);
		return dt1.isEqual(dt2);
	}

	/**
	 * Compares two datetimes and returns true iff the first datetime (dt1) is
	 * before or equal to the second datetime (dt2). Both datetimes are represented
	 * as six integers each, referring to years, months, days, hours, minutes and seconds
	 * respectively.
	 * 
	 * @param dt1Year   the year field for dt1
	 * @param dt1Month  the month field for dt1
	 * @param dt1Day    the day field for dt1
	 * @param dt1Hour   the hour field for dt1
	 * @param dt1Minute the minute field for dt1
	 * @param dt1Second the second field for dt1
	 * @param dt2Year   the year field for dt2
	 * @param dt2Month  the month field for dt2
	 * @param dt2Day    the day field for dt2
	 * @param dt2Hour   the hour field for dt2
	 * @param dt2Minute the minute field for dt2
	 * @param dt2Second the second field for dt2
	 * @return true if dt1 is before dt2 in time or both dt1 and dt2 denote the same
	 *         point in time, false otherwise
	 */
	@Predicate(name = "stdlib_datetime_is_before_or_equal")
	public static boolean datetimeIsBeforeOrEqual(int dt1Year, int dt1Month, int dt1Day, int dt1Hour, int dt1Minute, int dt1Second,
			int dt2Year, int dt2Month, int dt2Day, int dt2Hour, int dt2Minute, int dt2Second) {
		LocalDateTime dt1 = LocalDateTime.of(dt1Year, dt1Month, dt1Day, dt1Hour, dt1Minute, dt1Second);
		LocalDateTime dt2 = LocalDateTime.of(dt2Year, dt2Month, dt2Day, dt2Hour, dt2Minute, dt2Second);
		return dt1.isBefore(dt2) || dt1.isEqual(dt2);
	}

	/**
	 * Formats a datetime value represented using six integers as a string according
	 * to the given pattern. Valid format trings are those accepted by
	 * {@link DateTimeFormatter.ofPattern}.
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hours
	 * @param minutes
	 * @param seconds
	 * @param format
	 * @return a string representing the given datetime in the format specified by
	 *         the format string
	 */
	@Predicate(name = "stdlib_datetime_to_string")
	public static Set<List<ConstantTerm<String>>> datetimeToString(int year, int month, int day, int hours, int minutes, int seconds, String format) {
		LocalDateTime datetime = LocalDateTime.of(year, month, day, hours, minutes, seconds);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return Collections.singleton(Terms.asTermList(formatter.format(datetime)));
	}

	/**
	 * Checks whether the given string matches the given regex.
	 */
	@Predicate(name = "stdlib_string_matches_regex")
	public static boolean stringMatchesRegex(String str, String regex) {
		return str.matches(regex);
	}

	/**
	 * Returns the length of the given string
	 */
	@Predicate(name = "stdlib_string_length")
	public static Set<List<ConstantTerm<Integer>>> stringLength(String str) {
		return Collections.singleton(Terms.asTermList(str.length()));
	}

	/**
	 * Concatenates the two given strings
	 */
	@Predicate(name = "stdlib_string_concat")
	public static Set<List<ConstantTerm<String>>> stringConcat(String s1, String s2) {
		return Collections.singleton(Terms.asTermList(s1 + s2));
	}

}
