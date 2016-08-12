/*** Navi-h2o
 */
package com.navigosgroup.ml.h2o

import java.io._
import scala.io.Source
import hex.Model
import hex.Model.Output
import org.apache.spark.h2o._
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkContext
import water.support._
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import scala.reflect.runtime.universe

/**
 * @author Ninhsth
 *
 * This application use word2vec to build a model
 * classifying job at VNW.
 * 
 */
class JobRoleApp(jobsFile: String = JobRoleApp.DATA_FILE)
(@transient override val sc: SparkContext,
		@transient override val sqlContext: SQLContext,
		@transient override val h2oContext: H2OContext) extends SparklingWaterApp 
		with SparkContextSupport with GBMSupport with ModelMetricsSupport with H2OFrameSupport with Serializable {

	// Import companion object methods
	import JobRoleApp._
	

	// Build models and predict a few job titles
	def run(): Unit = {
			// Get training frame and word to vec model for data
			println("Start of run(): ")
			val (gbmModel, w2vModel) = buildModels(jobsFile, "modelX")
			println("buildModels finished: ")

			val classNames = gbmModel._output.asInstanceOf[Output].classNames()

			val block : String => Unit = x => { 
				//println(show(x, predict(x, gbmModel, w2vModel), classNames))
				//println("*** above is predicted role for: " + x)
				print2File(show(x, predict(x, gbmModel, w2vModel), classNames))
			}
			TEST_PREDICT_ROLES.foreach(block)

	}


	def buildModels(datafile: String = jobsFile, modelName: String): (Model[_,_,_], Word2VecModel) = {
		// Get training frame and word to vec model for data
		val (allDataFrame, w2vModel) = createH2OFrame(DATA_FILE)    
				val frs = splitFrame(allDataFrame, Array("train.hex", "valid.hex"), Array(0.75, 0.25))
				val (trainFrame, validFrame) = (h2oContext.asH2OFrame(frs(0)), h2oContext.asH2OFrame(frs(1)))
				println("splitFrame finished: ")

				val gbmModel = GBMModel(trainFrame, validFrame,  OUTPUT_COL, modelName, ntrees = 50)
				println("GBMModel finished: ")

				// Cleanup
				Seq(trainFrame, validFrame, allDataFrame).foreach(_.delete)

				(gbmModel, w2vModel)
	}


	def predict(jobTitle: String, modelId: String, w2vModel: Word2VecModel): (String, Array[Double]) = {
		predict(jobTitle, model = water.DKV.getGet(modelId), w2vModel)
	}

	def predict(jobTitle: String, model: Model[_,_,_], w2vModel: Word2VecModel): (String, Array[Double]) = {
		val tokens = tokenize(jobTitle, STOP_WORDS)
				val vec = wordsToVector(tokens, w2vModel)

				// FIXME should use Model#score(double[]) method but it is now wrong and need to be fixed
				import sqlContext.implicits._
				import h2oContext.implicits._
				val frameToPredict: H2OFrame = sc.parallelize(Seq(vec)).map(v => JobOffer(null, v)).toDF
				frameToPredict.remove(0).remove()
				val prediction = model.score(frameToPredict)
				// Read predicted classifiedRole
				val predictedCategory = prediction.vec(0).factor(prediction.vec(0).at8(0))
				// Read probabilities for each classifiedRole
				val probs = 1 to prediction.vec(0).cardinality() map (idx => prediction.vec(idx).at(0))
				// Cleanup
				Seq(frameToPredict, prediction).foreach(_.delete)
				(predictedCategory, probs.toArray)
	}

	def classify(jobTitle: String, modelId: String, w2vModel: Word2VecModel): (String, Array[Double]) = {
		classify(jobTitle, model = water.DKV.getGet(modelId), w2vModel)

	}

	def classify(jobTitle: String, model: Model[_,_,_], w2vModel: Word2VecModel): (String, Array[Double]) = {
		val tokens = tokenize(jobTitle, STOP_WORDS)
				if (tokens.length == 0) {
					EMPTY_PREDICTION
				} else {
					val vec = wordsToVector(tokens, w2vModel)

							hex.ModelUtils.classify(vec.toArray, model)
				}
	}

	def createH2OFrame(datafile: String): (H2OFrame, Word2VecModel) = {
		// Load data from specified file
		val dataRdd = loadData(DATA_FILE)

				// Compute rare words
				val tokenizedRdd = dataRdd.map(d => (d(0), tokenize(d(1), STOP_WORDS)))
				.filter(s => s._2.length > 0)
				// Compute rare words
				val rareWords = computeRareWords(tokenizedRdd.map(r => r._2))
				// Filter all rare words
				val filteredTokenizedRdd = tokenizedRdd.map(row => {
					val tokens = row._2.filterNot(token => rareWords.contains(token))
							(row._1, tokens)
				}).filter(row => row._2.length > 0)

				// Extract words only to create Word2Vec model
				val words = filteredTokenizedRdd.map(v => v._2.toSeq)

				// Create a Word2Vec model
				val w2vModel = new Word2Vec().fit(words)

				// Sanity Check    
				val block : String => Unit = x => { 
					println("*** Synonyms for " + x)
					w2vModel.findSynonyms(x, 5).foreach(println)
				}
				TEST_SYNONYM_WORDS.foreach(block)

				// Create vectors

				val finalRdd = filteredTokenizedRdd.map(row => {
					val label = row._1
							val tokens = row._2
							// Compute vector for given list of word tokens, unknown words are ignored
							val vec = wordsToVector(tokens, w2vModel)
							JobOffer(label, vec)
				})

				// Transform RDD into DF and then to HF
				import h2oContext.implicits._
				import sqlContext.implicits._
				val h2oFrame: H2OFrame = finalRdd.toDF
				println("OUTPUT_COL:"+OUTPUT_COL)
				h2oFrame.replace(h2oFrame.find(OUTPUT_COL), h2oFrame.vec(OUTPUT_COL).toCategoricalVec).remove()

				(h2oFrame, w2vModel)
	}

	def computeRareWords(dataRdd : RDD[Array[String]]): Set[String] = {
		// Compute frequencies of words
		val wordCounts = dataRdd.flatMap(s => s).map(w => (w,1)).reduceByKey(_ + _)

				// Collect rare words
				val rareWords = wordCounts.filter { case (k, v) => v < 2 }
		.map {case (k, v) => k }
		.collect
		.toSet
		rareWords
	}
	def show(input: String, pred: (String, Array[Double]), classNames: Array[String]): String = {
		val probs = classNames.zip(pred._2).map(v => f"${v._1}: ${v._2}%.3f")
				input + " ==> " + pred._1 + ":" + pred._2.reduceLeft(_ max _) + probs.mkString("[", ", ", "]")
	}
	// Load data via Spark API
	private def loadData(filename: String): RDD[Array[String]] = {
		//val newdata = sc.parallelize(Array["test"])
		val data = sc.textFile(filename)
				.filter(line => !line.contains(OUTPUT_COL)).map(_.split(','))

				//    val distinctData = data.map(_.map(Set(_))).reduce { 
				//            (a, b) => (a.zip(b)).map { case (x, y) => x}}
				//    val distinctData = data.map(_.map(Set(_))).reduce { 
				//            (a, b) => (a.zip(b)).map { case (x, y) => x}}
				//    distinctData.foreach { println}   

				data
	}
}

/**
 * Representation of single job offer with its classification.
 *
 * @param classifiedRole  job role (Mobile Developer, Engineering Manager, ...)
 * @param fv  feature vector describing job role
 */
case class JobOffer(classifiedRole: String, fv: org.apache.spark.mllib.linalg.Vector)

object JobRoleApp extends SparkContextSupport {
	val DATA_FILE="examples/smalldata/vnw-it-jobs-role.csv"
			val RESULT_FILE="output/result.csv"
			val OUTPUT_COL="classifiedRole"

			val EMPTY_PREDICTION = ("NA", Array[Double]())

			val STOP_WORDS = Set("ax","i","you","edu","s","t","m","subject","can","lines","re","what"
					,"there","all","we","one","the","a","an","of","or","in","for","by","on"
					,"but", "is", "in","a","not","with", "as", "was", "if","they", "are", "this", "and", "it", "have"
					, "from", "at", "my","be","by","not", "that", "to","from","com","org","like","likes","so"
					// Ninh added
					,"vacancies", "up", "usd", "exp", "welcome", "urgent", "good", "new", "attractive", "___"
					,"years", "year", "salary", "need", "needed", "very")

					val ROLES = Set("Account Manager",
							"Backend Developer",
							"Business Development",
							"Content Creator",
							"Creative Director",
							"Data Scientist",
							"Designer",
							"Developer",
							"DevOps",
							"Engineering Manager",
							"Frontend Developer",
							"Full Stack Developer",
							"Manager Business Development",
							"Marketing",
							"Mobile Developer",
							"Product Manager",
							"QA Engineer",
							"Sales",
							"Sales Manager",
							"Software Architect",
							"UI/UX Designer",
							"User Experience Design",
							"User Researcher",
							"Visual Designer")

							val TEST_SYNONYM_WORDS = Set("java", "php", "designer","tester", "engineer")
							val TEST_PREDICT_ROLES = Set("Kỹ Sư CNTT -: "
									// , "!!!hot! developer with 3+ Java experience, jumping"
									//   , "System Test Developer,Linux"
									//    , "needed Ruby DevOps Engineer"
									//    , "urgent! Architect/team Leader,User Interface Design"
									//    , "PHP Developer (Không Cần Kinh Nghiệm"
									//     , "ASP.NET Web Developer "
									//    , "(Junior!) Microsoft SQL BI and Data Warehouse Developer"
									, "!!! urgent senior product manager"
									, "junior data analyst"
									//     , "[urgent] 15 Java Developers (junior-senior)"
									//     , "Urgent_project Manager - Java - 2 Positions"
									//    , "kỹ sư điện tử viễn thông"
									//    , "*urgent* 5 C++ Software Engineers [fresher])"
									, "senior Data Scientist"
									)

									val JOB_TITLE_FILE="input/jobtitle.csv"    
								
									

									def main(args: Array[String]): Unit = {
		//addTitlesFromRoles()

		// Prepare environment
		val sc = new SparkContext(configure("JobRoleApp"))
		val sqlContext = new SQLContext(sc)
		// Start H2O services
		val h2oContext = H2OContext.getOrCreate(sc)

		val app = new JobRoleApp(DATA_FILE)(sc, sqlContext, h2oContext)
		try {
			app.run()
		} catch {
		case e: Throwable =>
		e.printStackTrace()
		//fw.close()
		throw e
		} finally {
			app.shutdown()
			//fw.close()
		}
	}

	private[h2o] def tokenize(line: String, stopWords: Set[String]) = {
		//get rid of nonWords such as punctuation as opposed to splitting by just " "
		line.split("""\W+""")
		// Unify
		.map(_.toLowerCase)

		//remove mix of words+numbers
		.filter(word => ! (word exists Character.isDigit) )

		//remove stopwords defined above (you can add to this list if you want)
		.filterNot(word => stopWords.contains(word))

		//leave only words greater than 1 characters.
		//this deletes A LOT of words but useful to reduce our feature-set
		.filter(word => word.size >= 2)
	}

	// Make some helper functions
	private def sumArray (m: Array[Double], n: Array[Double]): Array[Double] = {
		for (i <- 0 until m.length) {m(i) += n(i)}
		return m
	}

	private def divArray (m: Array[Double], divisor: Double) : Array[Double] = {
		for (i <- 0 until m.length) {m(i) /= divisor}
		return m
	}

	private[h2o] def wordsToVector(words: Array[String], model: Word2VecModel): Vector = {
		try {val vec = Vectors.dense(
				divArray(
						words.map(word => wordToVector(word, model).toArray).reduceLeft(sumArray),
						words.length))
						vec
		} catch {
		case e: Exception => return Vectors.zeros(100)
		}
	}
	private def wordToVector(word: String, model: Word2VecModel): Vector = {
		try {
			return model.transform(word)
		} catch {
		case e: Exception => return Vectors.zeros(100)
		}
	}

	def show(input: String, pred: (String, Array[Double]), classNames: Array[String]): String = {
		val probs = classNames.zip(pred._2).map(v => f"${v._1}: ${v._2}%.3f")
				input + " ==> " + pred._1 + ":" +pred._2.reduceLeft(_ max _) + probs.mkString("[", ", ", "]")
	}


	// to increase score of standard job titles (same as job roles)
	def addTitlesFromRoles(){
		val fw = new FileWriter(DATA_FILE, true) ; 
		val block : String => Unit = x => { 
			fw.write("\n")  
			fw.write(x+","+x) 
		}
		for( a <- 1 to 1000){
			ROLES.foreach(block) 
		}
		fw.close()
		System.exit(0)
	}

	def print2File(str: String){
		//fw = new FileWriter(RESULT_FILE, true) ;    
		// fw.write("\n"+str) 
		//fw.close()
	}
}
