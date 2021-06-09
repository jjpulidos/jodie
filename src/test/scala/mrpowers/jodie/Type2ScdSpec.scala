package mrpowers.jodie

import org.scalatest.FunSpec
import java.sql.{Timestamp, Date}
import org.apache.spark.sql.types._

class Type2ScdSpec extends FunSpec with SparkSessionTestWrapper {

  import spark.implicits._

  describe("upsert") {
    it("upserts a Delta Lake with a single attribute") {
      val path = (os.pwd / "tmp" / "delta-upsert").toString()
      // create Delta Lake
      val df = Seq(
        (1, "A", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
        (2, "B", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
        (4, "D", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
      ).toDF("pkey", "attr", "is_current", "effective_time", "end_time")
        .withColumn("end_time", $"end_time".cast(TimestampType))
      df.write.format("delta").save(path)
      // create updates DF
      val updatesDF = Seq(
        (2, "Z", Timestamp.valueOf("2020-01-01 00:00:00")), // value to upsert
        (3, "C", Timestamp.valueOf("2020-09-15 00:00:00")), // new value
      ).toDF("pkey", "attr", "effective_time")
      // perform upsert
      Type2Scd.upsert(path, updatesDF, "pkey", Seq("attr"))
      // show result
      spark.read.format("delta").load(path).show()
    }

    it("upserts based on multiple attributes") {
      val path = (os.pwd / "tmp" / "delta-upsert2").toString()
      // create Delta Lake
      val df = Seq(
        (1, "A", "A", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
        (2, "B", "B", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
        (4, "D", "D", true, Timestamp.valueOf("2019-01-01 00:00:00"), null),
      ).toDF("pkey", "attr1", "attr2", "is_current", "effective_time", "end_time")
        .withColumn("end_time", $"end_time".cast(TimestampType))
      df.show()
      df.write.format("delta").save(path)
      // create updates DF
      val updatesDF = Seq(
        (2, "Z", null, Timestamp.valueOf("2020-01-01 00:00:00")), // value to upsert
        (3, "C", "C", Timestamp.valueOf("2020-09-15 00:00:00")), // new value
      ).toDF("pkey", "attr1", "attr2", "effective_time")
      updatesDF.show()
      // perform upsert
      Type2Scd.upsert(path, updatesDF, "pkey", Seq("attr1", "attr2"))
      // show result
      spark.read.format("delta").load(path).show()
    }
  }

  describe("genericUpsert") {
    it("upserts based on date columns") {
      val path = (os.pwd / "tmp" / "delta-upsert-date").toString()
      // create Delta Lake
      val df = Seq(
        (1, "A", true, Date.valueOf("2019-01-01"), null),
        (2, "B", true, Date.valueOf("2019-01-01"), null),
        (4, "D", true, Date.valueOf("2019-01-01"), null),
      ).toDF("pkey", "attr", "cur", "effective_date", "end_date")
        .withColumn("end_date", $"end_date".cast(DateType))
      df.write.format("delta").save(path)
      // create updates DF
      val updatesDF = Seq(
        (2, "Z", Date.valueOf("2020-01-01")), // value to upsert
        (3, "C", Date.valueOf("2020-09-15")), // new value
      ).toDF("pkey", "attr", "effective_date")
      // perform upsert
      Type2Scd.genericUpsert(path, updatesDF, "pkey", Seq("attr"), "cur", "effective_date", "end_date")
      // show result
      spark.read.format("delta").load(path).show()
    }

    it("upserts based on version number") {
      val path = (os.pwd / "tmp" / "delta-upsert-ver").toString()
      // create Delta Lake
      val df = Seq(
        (1, "A", true, 1, null),
        (2, "B", true, 1, null),
        (4, "D", true, 1, null),
      ).toDF("pkey", "attr", "is_current", "effective_ver", "end_ver")
        .withColumn("end_ver", $"end_ver".cast(IntegerType))
      df.write.format("delta").save(path)
      // create updates DF
      val updatesDF = Seq(
        (2, "Z", 2), // value to upsert
        (3, "C", 3), // new value
      ).toDF("pkey", "attr", "effective_ver")
      // perform upsert
      Type2Scd.genericUpsert(path, updatesDF, "pkey", Seq("attr"), "is_current", "effective_ver", "end_ver")
      // show result
      spark.read.format("delta").load(path).show()
    }
  }

}
