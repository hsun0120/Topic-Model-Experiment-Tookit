require("stm", quietly=TRUE)
require("tm", quietly=TRUE)
require("profvis", quietly=TRUE)

experiment <- function(mPath, vPath, out, k) {
  #Read matrix from file system in LDA-C format
  dtm <- read_dtm_Blei_et_al(mPath, vocab = vPath)
  #Construct term-document matrix
  mat <- readCorpus(dtm, type = c("slam"))
  processed <- prepDocuments(mat$documents, mat$vocab)
  save(processed, file=paste0(out, "/tdm.RData"))
    
  sink(paste0(out, "/stm"), type = c("output"))
  
  #Get profiling data
  p <- profvis({
    res <<- stm(processed$documents, processed$vocab, K = k, max.em.its = 200, init.type = "Spectral")
  }, interval=0.2)
  htmlwidgets::saveWidget(p, "profile.html", selfcontained=FALSE)
  
  #Get stm output and top 10 words
  print(res)
  lt <- labelTopics(res, n = 10)
  print(lt)
  sink()
  
  #Output theta matrix
  write.csv(res$theta, file=paste0(out, "/theta.csv"))
  corr <- topicCorr(res)
  write.csv(corr$cor, paste0(out, "/cor.csv"))
  
  #Output semantic coherence graph
  fname <- paste0(out, "/corr.png")
  png(filename=fname)
  plot(corr)
  dev.off()
  
  #Output semantic coherence-exclusiveness graph
  fname <- paste0(out, "/exclCor.png")
  png(filename=fname)
  topicQuality(res, processed$documents)
  dev.off()
  
  return("done")
}
