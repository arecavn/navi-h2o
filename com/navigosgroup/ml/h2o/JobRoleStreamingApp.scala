/*** Navi-h2o
 */

package com.navigosgroup.ml.h2o

import java.net.URI

import hex.Model.Output
import org.apache.spark.SparkContext
import org.apache.spark.h2o._
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import water.support.{ModelSerializationSupport, SparkContextSupport}

/**
 * @author Ninhsth
 * Variant of JobRole App with streaming support to classify
 * incoming events.
 *
 * Launch: nc -lk 9999
 * and send events from your console
 *
 * Send "poison pill" to kill the application.
 *
 */
object JobRoleStreamingApp extends SparkContextSupport with ModelSerializationSupport {

  val POISON_PILL_MSG = "poison pill"

  def main(args: Array[String]) {
    // Prepare environment
    val sc = new SparkContext(configure("JobRoleApp"))
    val ssc = new StreamingContext(sc, Seconds(10))
    val sqlContext = new SQLContext(sc)
    // Start H2O services
    val h2oContext = H2OContext.getOrCreate(sc)

    // Build an initial model
    val staticApp = new JobRoleApp()(sc, sqlContext, h2oContext)
    try {
      val (svModel, w2vModel) = staticApp.buildModels(JobRoleApp.DATA_FILE, "jobRoleModel")
      val modelId = svModel._key.toString
      val classNames = svModel._output.asInstanceOf[Output].classNames()

      // Lets save models
      exportSparkModel(w2vModel, URI.create("file:///tmp/sparkmodel"))
      exportH2OModel(svModel, URI.create("file:///tmp/h2omodel/"))

      // Start streaming context
      val jobTitlesStream = ssc.socketTextStream("localhost", 9999)

      // Classify incoming messages
      jobTitlesStream.filter(!_.isEmpty)
        .map(jobTitle => (jobTitle, staticApp.classify(jobTitle, modelId, w2vModel)))
        .map(pred => "\"" + pred._1 + "\" = " +  JobRoleApp.show("",pred._2, classNames))
        .print()

      // Shutdown app if poison pill is passed as a message
      jobTitlesStream.filter(msg => POISON_PILL_MSG == msg)
        .foreachRDD(rdd => if (!rdd.isEmpty()) {
          println("Poison pill received! Application is going to shut down...")
          ssc.stop(true, true)
          staticApp.shutdown()
      })

      println("Please start the event producer at port 9999, for example: nc -l -p 9999")
      ssc.start()
      ssc.awaitTermination()

    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
      ssc.stop()
      staticApp.shutdown()
    }
  }

}

