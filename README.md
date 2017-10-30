# DCRecyclerView
实现接口
    public interface DCListener{
    
    
        item和距离的关系
        
        
        void onChangeDistance(View view,float percentX,float percentY,int firstItemPosition,int lastItemPosition,int count);
        
        
        向上下左右滚动状态
        
        
        void scrollUp();
        void scrollDown();
        void scrollLeft();
        void scrollRight();
        
        
        到达顶底左右监听
        
        
        void onStop();
        void onReachTop();
        void onReachBottom();
        void onReachLeft();
        void onReachRight();
        
        
        中心监听
        
        
        void reachCenter(int index);
    }
