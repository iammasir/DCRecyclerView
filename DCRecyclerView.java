

import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 *
 */

public class DCRecyclerView extends RecyclerView {

    //********************
    public boolean listenItemsDistance=true;

    private float factor=0.5f;//影响结果的因子，数值越大层次越明显。数值越小层次越缓和
    private float v=0.5f;//RecyclerView 滑动速度控制
    private int mCurrentScrollY;//rv滑动的总的Y轴总距离，如果item的距离是固定的，那么可以计算出中间的位置
    private int mCurrentScrollX;//rv滑动的总的X轴总距离，如果item的距离是固定的，那么可以计算出中间的位置
    private int rvHeight=-1;//rv的可见部分的高度
    private int rvWidth=-1;//rv的可见部分的宽度
    private int itemHeight=-1;//item高度
    private int itemWidth=-1;//item宽度
    private LinearLayoutManager linearLayoutManager;
    private LinearSnapHelper linearSnapHelper;


    private int centerIndex=-1;
    private int originalOffset=-1;

    //********************
    private final int UP=1;
    private final int STOP=0;
    private final int DOWN=-1;
    private final int LEFT=-2;
    private final int RIGHT=2;
    private int scrollState=STOP;

    private boolean isFirst=true;//是否第一次执行
    private boolean isThreadStart = false;//线程是否开启
    private int i = 0;//计时器可调的值

    private DCListener listener;





    public DCRecyclerView(Context context) {
        super(context);
    }

    public DCRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DCRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setDCListener(DCListener listener) {
        this.listener = listener;
    }

    public interface DCListener{
        void onChangeDistance(View view,float percentX,float percentY,int firstItemPosition,int lastItemPosition,int count);
        void scrollUp();
        void scrollDown();
        void scrollLeft();
        void scrollRight();
        void onStop();
        void onReachTop();
        void onReachBottom();
        void onReachLeft();
        void onReachRight();
        void reachCenter(int index);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);

        if (listenItemsDistance){//此方法默认不监控item位置，因为会使得rv功能受到一定限制
            getItemDistance(dx,dy);
        }

        getScrollState(dx,dy);
    }

    /**
     * 获取RV的滑动状态
     * @param dx
     * @param dy
     */
    private void getScrollState(int dx, int dy) {

        if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){
            if (listener!=null&&reachTop()){
                listener.onReachTop();
            }
            if (listener!=null&&reachBottom()){
                listener.onReachBottom();
            }
        }
        if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){
            if (listener!=null&&reachLeft()){
                listener.onReachLeft();
            }
            if (listener!=null&&reachRight()){
                listener.onReachRight();
            }
        }


        if (isFirst){
            isFirst=false;
            return;
        }
        //竖向滑动
        if (dx==0){
            if (dy>0){
                if (scrollState!=UP){
                    scrollState=UP;
                    if (listener!=null){
                        listener.scrollUp();
                    }
                }
            }else {
                if (scrollState!=DOWN){
                    scrollState=DOWN;
                    if (listener!=null){
                        listener.scrollDown();
                    }
                }
            }
        }

        //横向滑动
        if (dy==0){
            if (dx>0){
                if (scrollState!=LEFT){
                    scrollState=LEFT;
                    if (listener!=null){
                        listener.scrollLeft();
                    }
                }
            }else {
                if (scrollState!=RIGHT){
                    scrollState=RIGHT;
                    if (listener!=null){
                        listener.scrollRight();
                    }
                }
            }
        }

        timeDown();
    }


    /**
     * 获取每一个item距离中心点的距离
     */
    private void getItemDistance(int dx, int dy) {
        mCurrentScrollY=computeVerticalScrollOffset();
        mCurrentScrollX=computeHorizontalScrollOffset();

        if (linearLayoutManager==null ){

            if (getLayoutManager().getClass().getName().contains("LinearLayoutManager")){
                linearLayoutManager=(LinearLayoutManager) getLayoutManager();
                if (factor>1f||factor<=0f){
                    factor=0.5f;
                }
            }else {
                throw new IllegalStateException("RecyclerView的LayoutManager必须为LinearLayoutManager！");
            }

        }

        if (linearSnapHelper==null){
            linearSnapHelper=new LinearSnapHelper(this);
            linearSnapHelper.attachToRecyclerView(this);
        }

        if (originalOffset==-1){
            getOriginalOffset();
        }


        int firstItemPosition= linearLayoutManager.findFirstVisibleItemPosition();
        int lastItemPosition= linearLayoutManager.findLastVisibleItemPosition();


        if (itemHeight==-1){//item的高度
            itemHeight=getChildAt(firstItemPosition).getHeight();
        }
        if (itemWidth==-1){//item的宽度
            itemWidth=getChildAt(firstItemPosition).getWidth();
        }
        if (rvHeight==-1){//rv的可见部分的高度
            rvHeight=getHeight();
        }
        if (rvWidth==-1){//rv的可见部分的宽度
            rvWidth=getWidth();
        }

        //偏移量，如果偏移量不为0，代表firstItemPosition已经有一部分滑出了屏幕外边，而偏移量就是滑出屏幕外的值
        int offsetY=  mCurrentScrollY-itemHeight*firstItemPosition;//Y轴偏移量
        int offsetX=  mCurrentScrollX-itemWidth*firstItemPosition;//X轴偏移量


        //依次找出第一个可见item到最后一个可见item分别偏离中心点有多远
        int count=0;

        for (int index=firstItemPosition;index<lastItemPosition+1;index++){

            //item中心到rv中心点的距离

            float disY=Math.abs(rvHeight*0.5f-itemHeight*0.5f+offsetY-itemHeight*count);

            if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){//竖直滚动
                if ((lastItemPosition-firstItemPosition+1)%2!=0 && centerIndex!=(lastItemPosition+firstItemPosition)/2){
                    centerIndex=(lastItemPosition+firstItemPosition)/2;
                    if (listener!=null){
                        listener.reachCenter(centerIndex);
                    }
                }

            }

            disY*=factor;

            float percentY=disY/(rvHeight*0.5f+itemHeight*0.5f);
            //************************************************************************

            float disX=Math.abs(rvWidth*0.5f-itemWidth*0.5f+offsetX-itemWidth*count);
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){//水平滚动
                //奇数个item，中心下标不和上次一致
                if ((lastItemPosition-firstItemPosition+1)%2!=0 && centerIndex!=(lastItemPosition+firstItemPosition)/2){
                    centerIndex=(lastItemPosition+firstItemPosition)/2;
                    if (listener!=null){
                        listener.reachCenter(centerIndex);
                    }
                }
            }
            disX*=factor;
            float percentX=disX/(rvWidth*0.5f+itemWidth*0.5f);

            View view=linearLayoutManager.findViewByPosition(index);

            if (view!=null&&listener!=null){

                if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){
                    listener.onChangeDistance(view,percentX,0,firstItemPosition,lastItemPosition,count);
                };

                if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){
                    listener.onChangeDistance(view,0,percentY,firstItemPosition,lastItemPosition,count);
                }

            }

            count++;
        }

    }


    /**
     * 获取原始滚动偏差值
     */
    private void getOriginalOffset() {

        for (int i=0;i<linearLayoutManager.findLastVisibleItemPosition();i++){
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){
                if (i*itemHeight+0.5*itemHeight>=rvHeight*0.5){
                    originalOffset=(int) Math.min(Math.abs(i*itemHeight+0.5*itemHeight-rvHeight*0.5),Math.abs((i-1)*itemHeight+0.5*itemHeight-rvHeight*0.5));
                }
            }
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){

            }

        }
    }


    /**
     * 计时器,0.5秒内不执行第二次或者更多次，便执行结束的方法
     */
    public void timeDown() {
        if (!isThreadStart) {//方法执行的时候，线程只开启一次
            isThreadStart=true;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    for (; i < 5; i++) {//循环可运行0.5秒的时间
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    isThreadStart=false;
                    i=0;
                    //要执行的方法
                    if (listener!=null){
                        scrollState=STOP;
                        listener.onStop();


                    }

                }
            }.start();
        } else {
            i = 0;
        }
    }


    /**
     * 是否到达底部
     * @return
     */
    private boolean reachBottom() {
        if (computeVerticalScrollExtent()+computeVerticalScrollOffset()>= computeVerticalScrollRange()){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 是否到达顶端
     * @return
     */
    private boolean reachTop(){
        if (computeVerticalScrollOffset()==0){
            return true;
        }else {
            return false;
        }
    }


    /**
     * 是否到达右边
     * @return
     */
    private boolean reachRight() {
        if (computeHorizontalScrollExtent()+computeHorizontalScrollOffset()>= computeHorizontalScrollRange()){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 是否到达左边
     * @return
     */
    private boolean reachLeft(){
        if (computeHorizontalScrollOffset()==0){
            return true;
        }else {
            return false;
        }
    }


    /**
     * 在监听滑动距离的状态下，让下标为index的item滚动到中心位置
     * @param index
     */
    public void scrollItemToCenter(int index){
        if (listenItemsDistance){
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){

                float nowPosi= index*itemHeight+itemHeight*0.5f-mCurrentScrollY; //当前位置
                smoothScrollBy(0,(int)(nowPosi-rvHeight*0.5f));
            }

            if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){

                float nowPosi= index*itemWidth+itemWidth*0.5f-mCurrentScrollX; //当前位置
                smoothScrollBy((int)(nowPosi-rvWidth*0.5f),0);
            }

        }
    }


    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (listenItemsDistance){
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.VERTICAL){
                return super.fling(velocityX, (int) (v*velocityY));
            }
            if (linearLayoutManager.getOrientation()==LinearLayoutManager.HORIZONTAL){
                return super.fling((int) (v*velocityX), velocityY);
            }
        }
        return super.fling(velocityX, velocityY);

    }
}


