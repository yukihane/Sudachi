package com.worksap.nlp.sudachi;

import com.worksap.nlp.sudachi.dictionary.Grammar;
import com.worksap.nlp.sudachi.dictionary.WordInfo;

import java.io.IOException;
import java.util.*;

public class RewritePersonNameOovPlugin extends PathRewritePlugin {
    private short personNamePosId;
    private Set<String> prefix;
    private Set<String> suffix;
    private Set<String> prefixTexts;
    private Set<String> suffixTexts;
    private SortedSet<Integer> prefixTextLengths;
    private SortedSet<Integer> suffixTextLengths;

    @Override
    public void setUp(Grammar grammar) throws IOException {
        List<String> pos = settings.getStringList("personNamePOS");
        if (pos.isEmpty()) {
            throw new IllegalArgumentException("personNamePOS is undefined");
        }
        personNamePosId = grammar.getPartOfSpeechId(pos);
        if (personNamePosId < 0) {
            throw new IllegalArgumentException("personNamePOS is invalid");
        }
        prefix = new HashSet<>(settings.getPathList("prefix"));
        suffix = new HashSet<>(settings.getPathList("suffix"));
        setPrefixText(new HashSet<>(settings.getPathList("prefixText")));
        setSuffixText(new HashSet<>(settings.getPathList("suffixText")));
    }

    @Override
    public void rewrite(InputText<?> text, List<LatticeNode> path, Lattice lattice) {
        for (int i=0;i<path.size();i++){
            if (isTargetOov(path, text, i)){
                WordInfo info = path.get(i).getWordInfo();
                String surface = info.getSurface();
                short length = info.getLength();
                WordInfo rewritten = new WordInfo(surface, length, personNamePosId, surface, surface, "");
                path.get(i).setWordInfo(rewritten);
            }
        }
    }

    private boolean isTargetOov(List<LatticeNode> path, InputText<?> text, int index){
        if (path.get(index).isOOV()){
            return checkPrefixSuffix(path, index) || checkPrefixSuffixText(path, text, index);
        }
        return false;
    }

    private boolean checkPrefixSuffix(List<LatticeNode> path, int index){
        if (index > 0){
            WordInfo info = path.get(index-1).getWordInfo();
            return prefix.contains(info.getSurface());
        }
        if (index < path.size()-1){
            WordInfo info = path.get(index+1).getWordInfo();
            return suffix.contains(info.getSurface());
        }
        return false;
    }

    private boolean checkPrefixSuffixText(List<LatticeNode> path, InputText<?> text, int index){
        for(int length : prefixTextLengths){
            int end = path.get(index).getBegin();
            int begin = end - length;
            if(begin < 0) break;
            if (prefixTexts.contains(text.getSubstring(begin, end))) {
                return true;
            }
        }
        for(int length : suffixTextLengths){
            int begin = path.get(index).getEnd();
            int end = begin + length;
            if(end > text.getText().getBytes().length) break;
            if (suffixTexts.contains(text.getSubstring(begin, end))) {
                return true;
            }
        }
        return false;
    }

    void setPersonNamePosId(short pos){
        personNamePosId = pos;
    }

    void setPrefix(Set<String> prefix){
        this.prefix = prefix;
    }

    void setSuffix(Set<String> suffix){
        this.suffix = suffix;
    }

    void setPrefixText(Set<String> prefixTexts){
        this.prefixTexts = prefixTexts;
        initPrefixTextLengths(prefixTexts);
    }

    void setSuffixText(Set<String> suffixTexts) {
        this.suffixTexts = suffixTexts;
        initSuffixTextLengths(suffixTexts);
    }

    private void initPrefixTextLengths(Set<String> prefixTexts){
        SortedSet<Integer> set = new TreeSet<>();
        for (String text : prefixTexts){
            set.add(text.getBytes().length);
        }
        prefixTextLengths = set;
    }

    private void initSuffixTextLengths(Set<String> suffixTexts){
        SortedSet<Integer> set = new TreeSet<>();
        for (String text : suffixTexts){
            set.add(text.getBytes().length);
        }
        suffixTextLengths = set;
    }
}
