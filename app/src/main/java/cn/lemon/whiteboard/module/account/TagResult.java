package cn.lemon.whiteboard.module.account;

import java.util.List;

/**
 * 标签实体类
 */
public class TagResult {

    private List<KnowledgeBean> knowledge;
    private List<LevelBean> level;

    public List<KnowledgeBean> getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(List<KnowledgeBean> knowledge) {
        this.knowledge = knowledge;
    }

    public List<LevelBean> getLevel() {
        return level;
    }

    public void setLevel(List<LevelBean> level) {
        this.level = level;
    }

    public static class KnowledgeBean {
        /**
         * id : 20180904122030047755422387775256
         * name : 语文
         */

        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class LevelBean {
        /**
         * id : 20180904122111614756851593123100
         * name : 高一
         */

        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
