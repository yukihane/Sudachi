package com.worksap.nlp.sudachi;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RewritePersonNameOovPluginTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    JapaneseTokenizer tokenizer;
    RewritePersonNameOovPlugin plugin;

    @Before
    public void setUp() throws IOException {
        Utils.copyResource(temporaryFolder.getRoot().toPath(),
                "/personname/system.dic", "/user.dic", "/char.def", "/unk.def");
        String path = temporaryFolder.getRoot().getPath();
        String settings = Utils.readAllResource("/personname/sudachi.json");
        Dictionary dict = new DictionaryFactory().create(path, settings);
        tokenizer = (JapaneseTokenizer)dict.create();
        plugin = new RewritePersonNameOovPlugin();
    }

    @Test
    public void testSuffix(){
        List<String> pos = Arrays.asList("名詞","固有名詞","人名","一般", "*", "*");
        short posId = tokenizer.grammar.getPartOfSpeechId(pos);
        plugin.setPersonNamePosId(posId);
        plugin.setSuffixText(new HashSet<>());
        plugin.setPrefixText(new HashSet<>());
        plugin.setSuffix(new HashSet<>(Arrays.asList("様")));
        plugin.setPrefix(new HashSet<>());
        List<LatticeNode> path = getPath("林様");
        assertEquals(posId, path.get(0).getWordInfo().getPOSId());
    }

    @Test
    public void testPrefix(){
        List<String> pos = Arrays.asList("名詞","固有名詞","人名","一般", "*", "*");
        short posId = tokenizer.grammar.getPartOfSpeechId(pos);
        plugin.setPersonNamePosId(posId);
        plugin.setSuffixText(new HashSet<>());
        plugin.setPrefixText(new HashSet<>());
        plugin.setSuffix(new HashSet<>());
        plugin.setPrefix(new HashSet<>(Arrays.asList("Mr.")));
        List<LatticeNode> path = getPath("Mr.林");
        assertEquals(posId, path.get(1).getWordInfo().getPOSId());
    }

    @Test
    public void testSuffixText(){
        List<String> pos = Arrays.asList("名詞","固有名詞","人名","一般", "*", "*");
        short posId = tokenizer.grammar.getPartOfSpeechId(pos);
        plugin.setPersonNamePosId(posId);
        plugin.setSuffixText(new HashSet<>(Arrays.asList(" 様")));
        plugin.setPrefixText(new HashSet<>());
        plugin.setSuffix(new HashSet<>());
        plugin.setPrefix(new HashSet<>());
        List<LatticeNode> path = getPath("林 様");
        assertEquals(posId, path.get(0).getWordInfo().getPOSId());
    }

    @Test
    public void testPrefixText(){
        List<String> pos = Arrays.asList("名詞","固有名詞","人名","一般", "*", "*");
        short posId = tokenizer.grammar.getPartOfSpeechId(pos);
        plugin.setPersonNamePosId(posId);
        plugin.setSuffixText(new HashSet<>());
        plugin.setPrefixText(new HashSet<>(Arrays.asList("Mr. ")));
        plugin.setSuffix(new HashSet<>());
        plugin.setPrefix(new HashSet<>());
        List<LatticeNode> path = getPath("Mr. 林。");
        assertEquals(posId, path.get(2).getWordInfo().getPOSId());
    }

    @Test
    public void testSuffixMix(){
        List<String> pos = Arrays.asList("名詞","固有名詞","人名","一般", "*", "*");
        short posId = tokenizer.grammar.getPartOfSpeechId(pos);
        plugin.setPersonNamePosId(posId);
        plugin.setSuffixText(new HashSet<>(Arrays.asList(" 様お疲れ様です")));
        plugin.setPrefixText(new HashSet<>());
        plugin.setSuffix(new HashSet<>(Arrays.asList("様")));
        plugin.setPrefix(new HashSet<>());
        List<LatticeNode> path = getPath("林様、辻 様お疲れ様です。");
        assertEquals(posId, path.get(0).getWordInfo().getPOSId());
        assertEquals(posId, path.get(3).getWordInfo().getPOSId());
    }

    private List<LatticeNode> getPath(String text) {
        UTF8InputText input
                = new UTF8InputTextBuilder(text, tokenizer.grammar).build();
        LatticeImpl lattice = tokenizer.buildLattice(input);
        List<LatticeNode> path = lattice.getBestPath();
        plugin.rewrite(input, path, lattice);
        lattice.clear();
        return path;
    }
}
