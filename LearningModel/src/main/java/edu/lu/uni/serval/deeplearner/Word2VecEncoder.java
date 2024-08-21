package edu.lu.uni.serval.deeplearner;

import edu.lu.uni.serval.utils.FileHelper;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Word2VecEncoder {
  private static final Logger log = LoggerFactory.getLogger(Word2VecEncoder.class);

  private int windowSize = 4;

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }

  public void embedTokens(Path inputFile, int minWordFrequency, int layerSize, Path outputFileName) throws IOException {
    log.info("Load & Vectorize Sentences....");
        /*
            CommonPreprocessor will apply the following regex to each token: [\d\.:,"'\(\)\[\]|/?!;]+
            So, effectively all numbers, punctuation symbols and some special symbols are stripped off.
            Additionally it forces lower case for all tokens.
         */
    TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
    tokenizerFactory.setTokenPreProcessor(new MyTokenPreprocessor());

    log.info("****************Building model****************");
    SentenceIterator iter = new BasicLineIterator(inputFile.toFile());
    Word2Vec vec = new Word2Vec.Builder()
      .epochs(1)
//        		.batchSize(100)
//        		.useAdaGrad(reallyUse)
      .iterations(1)
      .learningRate(.01)
      .seed(50)
      .windowSize(windowSize)
      .minWordFrequency(minWordFrequency)
      .layerSize(layerSize)
      .iterate(iter)
      .tokenizerFactory(tokenizerFactory)
      .build();

    log.info("****************Fitting Word2Vec model****************");
    vec.fit();

    log.info("****************Writing word vectors to text file****************");
    // Write word vectors to file
    FileHelper.makeDirectory(outputFileName);
    WordVectorSerializer.writeWordVectors(vec, outputFileName.toFile());
    log.info("****************Finish off embedding****************\n");
  }

  public static void main(String[] args) throws FileNotFoundException {
    Path embeddingInputData = Paths.get("C:\\AI\\projects\\debug-method-name\\Data\\Output\\DL_Data", "embedding", "inputData.txt");
    SentenceIterator iter = new BasicLineIterator(embeddingInputData.toFile());
    TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
    tokenizerFactory.setTokenPreProcessor(new MyTokenPreprocessor());

    Word2Vec vec = new Word2Vec.Builder()
      .epochs(1)
//        		.batchSize(100)
//        		.useAdaGrad(reallyUse)
      .iterations(1)
      .learningRate(.01)
      .seed(50)
      .windowSize(4)
      .minWordFrequency(1)
      .layerSize(300)
      .iterate(iter)
      .tokenizerFactory(tokenizerFactory)
      .build();

    vec.fit();

//    if(iter.hasNext()) {
//      String sentence = iter.nextSentence();
//      System.out.println(sentence);
//      Tokenizer tokenizer = tokenizerFactory.create(sentence);
//
//      while(tokenizer.hasMoreTokens()) {
//        System.out.println(tokenizer.nextToken());
//      }
//    }
  }
}
