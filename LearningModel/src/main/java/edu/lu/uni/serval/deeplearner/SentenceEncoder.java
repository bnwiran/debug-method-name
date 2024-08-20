package edu.lu.uni.serval.deeplearner;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import edu.lu.uni.serval.utils.FileHelper;

public class SentenceEncoder {

	@SuppressWarnings("deprecation")
	public void encodeSentences(Path inputFile, int minWordFrequency, int layerSize, int windowSize, Path outputFileName) throws FileNotFoundException {
        SentenceIterator sentenceIterator = new BasicLineIterator(inputFile.toFile());
        AbstractCache<VocabWord> cache = new AbstractCache<>();

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new MyTokenPreprocessor()); // CommonPreprocessor

        /*
             if you don't have LabelAwareIterator handy, you can use synchronized labels generator
              it will be used to label each document/sequence/line with it's own label.
              But if you have LabelAwareIterator ready, you can provide it, for your in-house labels
        */
        LabelsSource source = new LabelsSource("SEN_");

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(minWordFrequency)
                .iterations(5)
                .epochs(1)
                .layerSize(layerSize)
                .learningRate(0.025)
                .labelsSource(source)
                .windowSize(windowSize)
                .iterate(sentenceIterator)
                .trainWordVectors(false)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .build();

        vec.fit();

        FileHelper.makeDirectory(outputFileName);
        String modelName = outputFileName.toString().substring(0, outputFileName.toString().lastIndexOf(".")) + ".zip";
        WordVectorSerializer.writeWord2VecModel(vec, new File(modelName));
        WordVectorSerializer.writeWordVectors(vec, outputFileName.toString());
	}

}
