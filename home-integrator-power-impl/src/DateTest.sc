import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")

LocalDate.parse("2019-3-2", formatter).atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond


