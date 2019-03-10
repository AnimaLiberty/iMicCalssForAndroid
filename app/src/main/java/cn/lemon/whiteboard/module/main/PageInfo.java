package cn.lemon.whiteboard.module.main;

public class PageInfo {

    private int vpPosition;// 对应ViewPager的索引  初始化完成后不在变动   0
    private int position;// 对应显示的索引 随删除与创建改变               0
    private boolean isCreate;// 是否被创建                                true


    // 是否第一页
    public boolean isFirstPage() {
        return position == 1;
    }

    // 是否最后一页
    public boolean isLastPage (){
        return position == PageIndexHelper.maxIndex;
    }

    //删除
    public void delete(){
        isCreate = false;
        position = PageIndexHelper.maxIndex;
    }

    //创建
    public void create(){
        isCreate = true;
    }

    public int getVpPosition() {
        return vpPosition;
    }

    public void setVpPosition(int vpPosition) {
        this.vpPosition = vpPosition;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        isCreate = create;
    }

}
