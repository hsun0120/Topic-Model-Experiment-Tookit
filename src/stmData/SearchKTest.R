require("stm", quietly=TRUE)
require("tm", quietly=TRUE)

searchKTest <- function(combType, peroid) {
  #Read matrix from file system in LDA-C format
  dtm <- read_dtm_Blei_et_al(paste0("mat", combType, ".ldac"), vocab = paste0("vocab", combType))
  #Construct term-document matrix
  mat <- readCorpus(dtm, type = c("slam"))
  processed <- prepDocuments(mat$documents, mat$vocab, lower.thresh = 5)
  #Run searchK function
  kresult <- searchK(processed$documents, processed$vocab, 0)
  
  #Output semantic coherence-exclusiveness graph
  fname <- paste0("diagnostic", combType, peroid, ".png")
  png(filename=fname)
  plot(kresult)
  dev.off()
  
  return("done")
}
