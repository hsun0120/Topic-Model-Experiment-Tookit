require("stm", quietly=TRUE)
require("tm", quietly=TRUE)

searchKTest <- function(combType, peroid) {
  #Read matrix from file system in LDA-C format
  dtm <- read_dtm_Blei_et_al(paste0("mat", combType, ".ldac"), vocab = paste0("vocab", combType))
  #Construct term-document matrix
  mat <- readCorpus(dtm, type = c("slam"))
  processed <- prepDocuments(mat$documents, mat$vocab, lower.thresh = 5)
  
  ptm <- proc.time()
  #Run searchK function
  kresult <- searchK(processed$documents, processed$vocab, c(0))
  print(proc.time() - ptm)
  
  #Output semantic coherence-exclusiveness graph
  fname <- paste0("diagnostic", combType, peroid, ".png")
  png(filename=fname)
  plot(kresult)
  dev.off()
  
  return("done")
}

args = commandArgs(trailingOnly=TRUE)
searchKTest(args[1], args[2])