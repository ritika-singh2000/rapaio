/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package rapaio.sandbox

import rapaio.io.CSV
import java.io.File
import rapaio.data.{Value, Index, Frame}
import rapaio.core.stat.ConfusionMatrix
import rapaio.ml.boosting.AdaBoostSAMMEClassifier
import rapaio.ml.tree.DecisionStumpClassifier
import rapaio.graphics.Plot
import rapaio.workspace.Workspace

/**
 * @author <a href="email:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
object CSVSandbox extends App {

  var df = CSV.read(
    file = new File("/home/ati/rapaio/rapaio-java/src/rapaio/datasets/titanic-train.csv"),
    header = true,
    typeHints = Map[String, String](
      ("PassengerId", "idx"),
      ("Survived", "nom"),
      ("Pclass", "nom"),
      ("SibSp", "nom"),
      ("Parch", "nom")
    ),
    naValues = List[String]("?", "", " ")
  )

  df = Frame.solid(df.rowCount,
    ("Sex", df.col("Sex")),
    ("Embarked", df.col("Embarked")),
    ("Pclass", df.col("Pclass")),
    //    ("Fare", df.col("Fare")),
    ("SibSp", df.col("SibSp")),
    //    ("Age", df.col("Age")),
    ("Parch", df.col("Parch")),
    ("Survived", df.col("Survived"))
  )

  val c = new AdaBoostSAMMEClassifier()
  c.weak = new DecisionStumpClassifier() {
    minCount = 10
  }
  c.learningRate = 1

  val index = new Index()
  val accuracy = new Value()

  for (runs <- 1 to 1000) {
    c.times = runs
    c.learnFurther(df, "Survived", c)
    c.predict(df)
    val acc = new ConfusionMatrix(df.col("Survived"), c.prediction).accuracy

    index.values ++ c.times
    accuracy.values ++ acc

    Workspace.draw(Plot().points(x = index, y = accuracy))
    Console.println()
  }
}
