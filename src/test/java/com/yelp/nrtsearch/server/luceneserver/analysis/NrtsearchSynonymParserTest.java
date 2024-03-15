/*
 * Copyright 2024 Yelp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yelp.nrtsearch.server.luceneserver.analysis;

import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.runner.RunWith;

@RunWith(RandomizedRunner.class)
public class NrtsearchSynonymParserTest extends LuceneTestCase {

  public void testParse() throws IOException, ParseException {
    Analyzer analyzer = new MockAnalyzer(random());
    NrtsearchSynonymParser parser =
        new NrtsearchSynonymParser(Boolean.TRUE, Boolean.TRUE, analyzer);
    String synonyms =
        "a, b|ix, pie-ix|plaza, pla\\xE7a|plaza, plz|str, strada|str, strasse|str, stra\\xDFe|village, vlg";
    parser.parse(new StringReader(synonyms));
    final SynonymMap map = parser.build();
    analyzer.close();

    analyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, true);
            return new TokenStreamComponents(
                tokenizer, new SynonymGraphFilter(tokenizer, map, true));
          }
        };

    assertAnalyzesTo(analyzer, "a", new String[] {"b", "a"}, new int[] {1, 0});
    assertAnalyzesTo(
        analyzer, "plaza", new String[] {"plaxe7a", "plz", "plaza"}, new int[] {1, 0, 0});
    assertAnalyzesTo(
        analyzer,
        "str",
        new String[] {"strada", "strasse", "straxdfe", "str"},
        new int[] {1, 0, 0, 0});

    analyzer.close();
  }

  public void testInvalidMappings() {
    Analyzer analyzer = new MockAnalyzer(random());
    NrtsearchSynonymParser parser =
        new NrtsearchSynonymParser(Boolean.TRUE, Boolean.TRUE, analyzer);
    String synonyms = "a, b, c, d, e";
    expectThrows(
        IllegalArgumentException.class,
        () -> {
          parser.parse(new StringReader(synonyms));
        });
    analyzer.close();
  }
}
