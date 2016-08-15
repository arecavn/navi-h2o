/*** Navi-h2o
 */

package com.navigosgroup.ml.h2o

import java.net.URI
import java.io._;
import scala.io.Source

import hex.Model
import hex.Model.Output
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.SparkContext
import org.apache.spark.h2o._
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import water.support.{ModelSerializationSupport, SparkContextSupport}

/**
 * @author Ninhsth
 * Variant of JobRole App with file support to classify text in file.
 * Can build model or just read from saved model files.
 */
object JobRoleFileApp extends SparkContextSupport with ModelSerializationSupport {
	val RESULT_COL_SEP=","
	
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
		  // Load model if exist
//		  var w2vModel = loadSparkModel(URI.create("file:////Users/Ninhsth/workspace/NaviClassifier/output/sparkmodel"))
//		  var svModel = loadH2OModel(URI.create("file:////Users/Ninhsth/workspace/NaviClassifier/output/h2omodel"))	 
//		  val modelId = svModel._key.toString
//      val classNames = svModel._output.asInstanceOf[Output].classNames()
       // Load models
      // - Load H2O model
      val h2oModel: Model[_, _, _] = loadH2OModel(new File("output/h2omodel").toURI)
      val modelId = h2oModel._key.toString
      val classNames = h2oModel._output.asInstanceOf[Output].classNames()
      // - Load Spark model
      val sparkModel = loadSparkModel[Word2VecModel](new File("output/sparkmodel").toURI)     
      
		  // Build model if not exist
//			val (svModel, w2vModel) = staticApp.buildModels(JobRoleApp.DATA_FILE, "initialModel")
//					val modelId = svModel._key.toString
//					val classNames = svModel._output.asInstanceOf[Output].classNames()
//
//					// Lets save models
//					exportSparkModel(w2vModel, URI.create("file:////Users/Ninhsth/workspace/NaviClassifier/output/sparkmodel"))
//					exportH2OModel(svModel, URI.create("file:////Users/Ninhsth/workspace/NaviClassifier/output/h2omodel/"))
      
      
      // Read job titles in file and predict then write to result file
			val fw = new FileWriter(JobRoleApp.RESULT_FILE)
// Print header
				fw.write("jobTitle" 
								+ RESULT_COL_SEP + "ClassifiedRole1" + RESULT_COL_SEP + "ClassifiedRole1Prob"
								+ RESULT_COL_SEP + "ClassifiedRole2" + RESULT_COL_SEP + "ClassifiedRole2Prob"
								+ RESULT_COL_SEP + "ClassifiedRole3" + RESULT_COL_SEP + "ClassifiedRole3Prob"
								+ RESULT_COL_SEP + classNames.mkString("", RESULT_COL_SEP, "" + "\n"))
      for(line <- Source.fromFile(JobRoleApp.JOB_TITLE_FILE).getLines()) {
				//println(show(line, predict(line, gbmModel, w2vModel), classNames)) 
				//print2File(show(line, predict(line, gbmModel, w2vModel), classNames)) 
				
				val (predictedClassName,top3Probs, predictedProbs) = staticApp.predict2(line, modelId, sparkModel)			
				// Print line: input and predicted probabilities
				//val (predictedClassName, predictedProbs)=staticApp.predict(line, modelId, sparkModel)
				fw.write(line.replace(",", " ") 
							+ RESULT_COL_SEP + classNames(predictedProbs.indexOf(top3Probs(0))) + RESULT_COL_SEP + top3Probs(0)
							+ RESULT_COL_SEP+ classNames(predictedProbs.indexOf(top3Probs(1))) + RESULT_COL_SEP + top3Probs(1)
							+ RESULT_COL_SEP+ classNames(predictedProbs.indexOf(top3Probs(2))) + RESULT_COL_SEP + top3Probs(2)
							+ RESULT_COL_SEP+ predictedProbs.mkString("", RESULT_COL_SEP, "" + "\n"))
				//fw.write("\n" + staticApp.show(line, staticApp.predict(line, modelId, sparkModel), classNames)); 
				fw.flush();
			}
			fw.close()
			ssc.stop(true, true)
			staticApp.shutdown()
		} catch {
		case e: Throwable => e.printStackTrace()
		} finally {
			ssc.stop()
			staticApp.shutdown()
		}
	}

}

