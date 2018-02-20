require("stm", quietly=TRUE)
require("tm", quietly=TRUE)
require("profvis", quietly=TRUE)

experiment <- function(combType) {
  #Read matrix from file system in LDA-C format
  dtm <- read_dtm_Blei_et_al(paste0("mat", combType, ".ldac"), vocab = paste0("vocab", combType))
  #Construct term-document matrix
  mat <- readCorpus(dtm, type = c("slam"))
  processed <- prepDocuments(mat$documents, mat$vocab, lower.thresh = 5)
  save(processed, file=paste0(tdm, combType, ".RData"))
    
  sink(paste0("stm", combType), type = c("output"))
  
  #Get profiling data
  p <- profvis({
    res <<- stm(processed$documents, processed$vocab, K = 0, init.type = "Spectral")
  }, interval=0.2)
  htmlwidgets::saveWidget(p, paste0("profile", combType, ".html", selfcontained=FALSE)
  
  #Get stm output and top 10 words
  print(res)
  lt <- labelTopics(res, n = 10)
  print(lt)
  sink()
  
  #Output theta matrix
  write.csv(res$theta, file=paste0("theta", combType, ".csv"))
  #Output topic correlation matrix
  corr <- topicCorr(res)
  write.csv(corr$poscor, paste0("cor", combType, ".csv"))
  
  #Output semantic coherence graph
  fname <- paste0("corr", combType, ".png")
  png(filename=fname)
  plot(corr)
  dev.off()
  
  #Output semantic coherence-exclusiveness graph
  fname <- paste0("exclCor", combType, ".png")
  png(filename=fname)
  topicQuality(res, processed$documents)
  dev.off()
}

args = commandArgs(trailingOnly=TRUE)
experiment(args[1])