# Topic-Model-Experiment-Tookit
Tookit for text analysis using stm. The tools including dependency extractors that extract subject-verb-object pairs from texts.

## Dependencies
1. pom.xml should have most dependencies.
2. HanLPDependencyExtractor requires HanLP
3. R script requires stm and tm packages.

## Get Dependency Pairs from Text:
1. Create six directories named "S", "V", "O", "SV", "VO", and "SO" under classpath.
2. Create a directory under classpath and put all text files in it.
3. Execute dependency generator, it takes the name of source directory as command line argument.
4. Run Dependency generator, and dependency pairs will be generated under those 6 directories.

## Execute Topic Model:
1. Run LDACMatrix to create term-document matrix and vocab for stm. LDACMatrix takes one argument, the base directory that contains those 6 directories. Matrix matXXX.dac and vocabXXX will be created under classpath.
2. Modify Experiment.sh under stmData to call R script (you may also call this function directly in R). For example, if the matrix name is matS.ldac and the vocab file name is vocabS:
   ```
   RScript ./Experiment.R S
   ```
