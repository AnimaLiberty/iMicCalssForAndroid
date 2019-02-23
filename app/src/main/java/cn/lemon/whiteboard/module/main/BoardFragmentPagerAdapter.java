package cn.lemon.whiteboard.module.main;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

import java.util.List;

/**
 * 画板界面适配器
 */

public class BoardFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private List<?> mFragment;
    private List<String> mTitleList;

    /**
     * 普通，主页使用
     */
    public BoardFragmentPagerAdapter(FragmentManager fm, List<?> mFragment) {
        super(fm);
        this.mFragment = mFragment;
    }

    /**
     * 接收首页传递的标题
     */
    public BoardFragmentPagerAdapter(FragmentManager fm, List<?> mFragment, List<String> mTitleList) {
        super(fm);
        this.mFragment = mFragment;
        this.mTitleList = mTitleList;
    }

    @Override
    public Fragment getItem(int position) {
        return (Fragment) mFragment.get(position);
    }

    //Remove a page for the given position. The adapter is responsible for removing the view from its container
    //防止重新销毁视图
//    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        //不注释这行会内存溢出
//        super.destroyItem(container, position, object);
    }

    @Override
    public int getCount() {
        return mFragment.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public void addFragmentList(List<?> fragment) {
        this.mFragment.clear();
        this.mFragment = null;
        this.mFragment = fragment;
        notifyDataSetChanged();
    }

}
