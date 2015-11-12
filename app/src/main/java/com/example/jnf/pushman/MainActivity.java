package com.example.jnf.pushman;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    CanvasView mView; //캔버스
    Point mPoTouch; //터치 포인트 좌표
    Bitmap[] mImage; //비트맵 이미지 배열
    int[][] mScreenImageType; //화면 아이콘 배치도
    int mGameLevelNum; //게임 스테이지 번호

    final int COUNT_SCREEN_IMAGE_ROW = 12; //화면 행 갯수
    final int COUNT_SCREEN_IMAGE_COL = 10; //화면 열 갯수

    final int LENGTH_SCREEN_START_X = 0; //화면 시작 포인트  X 좌표
    final int LENGTH_SCREEN_START_Y = 0; //화면 시작 포인트  Y 좌표
    int LENGTH_IMAGE_WIDTH = 48; //아이콘 이미지 넓이
    int LENGTH_IMAGE_HEIGHT = 48; //아이콘 이미지 높이
    final int MAX_GAME_LEVEL_NUM = 36; //최대 스테이지 번호

    final int IMAGE_TYPE_BACK = 0; //아이콘 배경
    final int IMAGE_TYPE_BLOCK = 1; //아이콘 벽
    final int IMAGE_TYPE_STONE = 2; //아이콘 돌
    final int IMAGE_TYPE_HOUSE_EMPTY = 3; //아이콘 빈집
    final int IMAGE_TYPE_HOUSE_FULL = 4; //아이콘 돌 & 집
    final int IMAGE_TYPE_PUSH_MAN = 5; //푸쉬맨
    final int IMAGE_TYPE_PUSH_MAN_IN_HOUSE = 6; //아이콘 푸쉬맨 & 집

    final int KEY_UP =0;    //UP버튼
    final int KEY_DOWN =1;  //DOWN 버튼
    final int KEY_LEFT =2;  //LEFT 버튼
    final int KEY_RIGHT =3; //RIGHT 버튼

    TextView mTextView;
    SoundPool mSoundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //상태바를 감추기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mTextView = (TextView)findViewById(R.id.CountText);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);

        //맴버변수 초기화
        initVariable();
        //캔버스를 그리는 클래스
        mView = new CanvasView(this);
        //레이아웃의 핸들을 가져와서 캔버스를 그림
        FrameLayout frame = (FrameLayout)findViewById(R.id.mainLayout);
        frame.addView(mView,0);

        //게임의 레벨을 매개변수로 받아서 화면을 다시 그림.
        LoadDataFile(mGameLevelNum);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 맴버 변수를 초기화 한다.
    public void initVariable(){
        int i=0, j = 0;
        mTextView.setText("0");
        //터치 포인트 좌표 생성
        mPoTouch = new Point(-1,-1);
        //BitMap 이미지 초기화
        mImage = new Bitmap[6];
        for(i =0; i < 6; i++) {
            mImage[i] = null;
        }

        //이미지 파일을 로딩해서 BItmap 배열에 저장
        mImage[IMAGE_TYPE_BACK] = BitmapFactory.decodeResource(getResources(),R.drawable.img_back);
        mImage[IMAGE_TYPE_BLOCK] = BitmapFactory.decodeResource(getResources(),R.drawable.img_block);
        mImage[IMAGE_TYPE_STONE] = BitmapFactory.decodeResource(getResources(),R.drawable.img_stone);
        mImage[IMAGE_TYPE_HOUSE_EMPTY] = BitmapFactory.decodeResource(getResources(),R.drawable.img_house_empty);
        mImage[IMAGE_TYPE_HOUSE_FULL] = BitmapFactory.decodeResource(getResources(),R.drawable.img_house_full);
        mImage[IMAGE_TYPE_PUSH_MAN] = BitmapFactory.decodeResource(getResources(),R.drawable.img_push_man);

        //지도 정보 배열 초기화
        mScreenImageType = new int[COUNT_SCREEN_IMAGE_ROW][COUNT_SCREEN_IMAGE_COL];

        for(j=0; j < COUNT_SCREEN_IMAGE_ROW; j++){
            for(i=0;i < COUNT_SCREEN_IMAGE_COL; i++){
                mScreenImageType[j][i] = 0;
            }
        }
        mGameLevelNum = 1;
    }

    protected class CanvasView extends View {
        public CanvasView(Context context){
            super(context);
        }

        public void onDraw(Canvas canvas){
            DrawScreenImage(canvas);
        }

        public Rect getScreenRect(){
            Rect rtScreen = new Rect();
            rtScreen.left = 0;
            rtScreen.right = this.getWidth();
            rtScreen.top = 0;
            rtScreen.bottom = this.getBottom();

            return rtScreen;
        }

        //지도 정보를 캔버스에 출력
        public void DrawScreenImage(Canvas canvas){
            //화면 영역 좌표를 구해서 반환
            Rect rtScreen = getScreenRect();

            //타일 조각이미지의 크기를 계산
            LENGTH_IMAGE_WIDTH = (int)(rtScreen.width() / COUNT_SCREEN_IMAGE_COL);
            LENGTH_IMAGE_HEIGHT = LENGTH_IMAGE_WIDTH;

            int sctStartX = rtScreen.left + LENGTH_SCREEN_START_X;
            int sctStartY = rtScreen.top + LENGTH_SCREEN_START_Y;
            int imageType = 0, posX = 0, posY = 0, width = 0, height = 0;

            for(int j=0; j < COUNT_SCREEN_IMAGE_ROW; j ++){
                for(int i = 0; i < COUNT_SCREEN_IMAGE_COL; i++){
                    imageType = mScreenImageType[j][i];

                    if(IMAGE_TYPE_PUSH_MAN_IN_HOUSE == imageType){
                        imageType = IMAGE_TYPE_PUSH_MAN;
                    }
                    posX = sctStartX + i * LENGTH_IMAGE_WIDTH;
                    posY = sctStartY + j * LENGTH_IMAGE_HEIGHT;
                    width = LENGTH_IMAGE_WIDTH;
                    height = LENGTH_IMAGE_HEIGHT;

                    canvas.drawBitmap(mImage[imageType],null, new Rect(posX,posY,posX + width, posY + height),null);

                }
            }
        }
    }

    //텍스트 파일을 읽어서 String으로 표시하는 함수
    public String ReadTextFile(String strFilePath){
        String text = null;
        try{
            //텍스트 파일 입력 스트링을 구한다.
            InputStream is = getAssets().open(strFilePath);

            //파일의 내용을 배열에 저장한다.
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            text = new String(buffer);

        }catch(Exception e){
            throw new RuntimeException(e);
        }

        return text;
    }

    //레벨 정보가 담긴 텍스트 파일을 가져와서 읽음.
    public void LoadDataFile(int levelNum){
        String bufFile = null;
        String filePath = null;
        mTextView.setText("0");

        //레벨 번호가 범위를 벗어나면 함수 탈출
        if(levelNum < 1){
            return;
        }else if(levelNum >  MAX_GAME_LEVEL_NUM){
            return;
        }

        //레벨 번호로 파일 경로를 생성
        filePath = String.format("data/stage_%d.txt", levelNum);
        //스테이지 정보 파일의 내용을 읽는다.
        bufFile = ReadTextFile(filePath);

        int pos = 0, row = 0, col = 0, length = 0, imageType = 0;
        String bufLine, bufItem;


        //파일 내용에서 타일 정보를 하나씩 추출해서 맴버 변수에 저장
        while((pos = bufFile.indexOf("\n")) >= 0 ){
            bufLine = bufFile.substring(0,pos);
            bufLine.trim();

            bufFile = bufFile.substring(pos + 1);
            length = bufFile.length();

            if(length <= 1){
                continue;
            }else if(length > COUNT_SCREEN_IMAGE_COL){
                length = COUNT_SCREEN_IMAGE_COL;
            }

            for(col = 0; col < length; col++){
                bufItem = bufLine.substring(col,col+1);
                imageType = Integer.parseInt(bufItem);
                mScreenImageType[row][col] = imageType;
            }

            row ++;

            if(COUNT_SCREEN_IMAGE_ROW <= row){
                break;
            }
        }

        mGameLevelNum = levelNum;
        String titleText;
        titleText = String.format("PushMan Level-%d",mGameLevelNum);
        this.setTitle(titleText);

        //캔버스를 갱신
        mView.invalidate();
    }

    //--------------PushMan 의 현재의 위치를 반환하는 함수
    public Point GetPositionPushMan(){
        int col = 0, row = 0 ;
        Point poPushMan = new Point();

        //지도 정보 배열 수직 아이콘 개수만큼 루프 반복
        for(row = 0; row < COUNT_SCREEN_IMAGE_ROW; row++){
            for(col = 0; col < COUNT_SCREEN_IMAGE_COL; col++){
                //푸쉬맨의 위치를 찾았다면 해당 위치를 반환
                if(mScreenImageType[row][col] == IMAGE_TYPE_PUSH_MAN || mScreenImageType[row][col] == IMAGE_TYPE_PUSH_MAN_IN_HOUSE){
                    poPushMan.x = col;
                    poPushMan.y = row;
                    return poPushMan;
                }
            }
        }
        return poPushMan;
    }

    //특정 위치에 돌구슬을 이동할수 있는지 판단.
    public boolean InsertStoneToCell(Point poCell){
        switch(mScreenImageType[poCell.y][poCell.x]){

            //배경일경우
            case IMAGE_TYPE_BACK :
                mScreenImageType[poCell.y][poCell.x] = IMAGE_TYPE_STONE;
                break;

            //빈집일경우
            case IMAGE_TYPE_HOUSE_EMPTY :
                mScreenImageType[poCell.y][poCell.x] = IMAGE_TYPE_HOUSE_FULL;
                break;
            default:
                return false;
        }
        return true;
    }

    //------------------푸쉬맨 또는 돌구슬 이 있던 자리를 비워주는 함수
    public void RecoverToEmptyCell(Point poCell){
        switch(mScreenImageType[poCell.y][poCell.x]){

            //돌구슬일때
            case IMAGE_TYPE_STONE:
                mScreenImageType[poCell.y][poCell.x] = IMAGE_TYPE_BACK;
                break;
            //돌구슬이 들어간 집일때
            case IMAGE_TYPE_HOUSE_FULL:
                mScreenImageType[poCell.y][poCell.x] = IMAGE_TYPE_HOUSE_EMPTY;
                break;
            //푸쉬맨일때
            case IMAGE_TYPE_PUSH_MAN:
                mScreenImageType[poCell.y][poCell.x] =IMAGE_TYPE_BACK;
                break;
            //푸쉬맨이 들어간 집일때
            case IMAGE_TYPE_PUSH_MAN_IN_HOUSE:
                mScreenImageType[poCell.y][poCell.x] = IMAGE_TYPE_HOUSE_EMPTY;
                break;
            default:
                break;
        }
    }

    //화살표 방향에 따라 푸쉬맨을 움직이는 함수
    public void MovePushMan(int keyType){
        Point poPushMan, poNewPush, poNewBall;
        poPushMan = GetPositionPushMan();
        poNewPush = new Point(poPushMan);
        poNewBall = new Point(poPushMan);

        switch(keyType){
            //화살표 위쪽
            case KEY_UP:
                poNewPush.y -= 1;
                poNewBall.y -= 2;
                break;
            //화살표 아래쪽
            case KEY_DOWN:
                poNewPush.y += 1;
                poNewBall.y += 2;
                break;
            //화살표 왼쪽
            case KEY_LEFT:
                poNewPush.x -= 1;
                poNewBall.x -= 2;
                break;
            //화살표 오른쪽
            case KEY_RIGHT:
                poNewPush.x += 1;
                poNewBall.x += 2;
                break;
        }

        switch(mScreenImageType[poNewPush.y][poNewPush.x]){
            //배경일때
            case IMAGE_TYPE_BACK:
                //전역변수 이미지 변경
                mScreenImageType[poNewPush.y][poNewPush.x] = IMAGE_TYPE_PUSH_MAN;
                break;
            //경계벽돌일때
            case IMAGE_TYPE_BLOCK:
                return;
            //돌구슬일때
            case IMAGE_TYPE_STONE:
                //돌구슬을 스크린 이미지 array의 지정된 셀에 삽입
                if(InsertStoneToCell(poNewBall)){
                    mScreenImageType[poNewPush.y][poNewPush.x] = IMAGE_TYPE_PUSH_MAN;
                }else{
                    return;
                }
                break;
            //빈집일때
            case IMAGE_TYPE_HOUSE_EMPTY:
                mScreenImageType[poNewPush.y][poNewPush.x] =IMAGE_TYPE_PUSH_MAN_IN_HOUSE;
                break;
            //공이 들어간 집일때
            case IMAGE_TYPE_HOUSE_FULL:
                //돌구슬을 스크린 이미지 array의 지정된 셀에 삽입
                //이동 가능할때
                if(InsertStoneToCell(poNewBall)){
                    mScreenImageType[poNewPush.y][poNewPush.x] = IMAGE_TYPE_PUSH_MAN_IN_HOUSE;
                }else{
                    return;
                }
                break;

        }
        //이동이 일어난후 빈자리를 메워줘야 한다.
        RecoverToEmptyCell(poPushMan);
        //다시 캔버스를 그림
        mView.invalidate();

        String strText, strValue;
        int intCount;
        strText = mTextView.getText().toString();

        intCount = Integer.parseInt(strText);
        intCount += 1;
        strValue = Integer.toString(intCount);

        mTextView.setText(strValue);

        int soundID;

        soundID = mSoundPool.load(this, R.raw.knock, 1);

        //playSoundPool(soundID);
        playSound_Delayed(soundID,500);
    }

    private void playSound_Delayed (final int soundId, final long millisec) {
        // TIMER
        final Handler mHandler = new Handler();
        final Runnable mDelayedTimeTask = new Runnable() {
            int counter = 0;
            public void run() {
                counter++;

                if (counter == 1) {
                    boolean ret = mHandler.postDelayed(this, millisec);
                    if (ret==false) Log.w("playSound_Delayed", "mHandler.postAtTime FAILED!");
                } else {
                    playSoundPool(soundId);
                }
            }
        };
        mDelayedTimeTask.run();
    }

    //-----------------버튼 클릭 이벤트
    public void onClick(View v){
        boolean gameComplete = false;
        switch(v.getId()){
            case R.id.buttonLevelPrev:
                LoadDataFile(mGameLevelNum - 1);
                break;
            case R.id.buttonLevelNext:
                LoadDataFile(mGameLevelNum + 1);
                break;
            case R.id.buttonMoveUp:
                MovePushMan(KEY_UP);
                gameComplete = IsGameComplete();
                break;
            case R.id.buttonMoveDown:
                MovePushMan(KEY_DOWN);
                gameComplete = IsGameComplete();
                break;
            case R.id.buttonMoveLeft:
                MovePushMan(KEY_LEFT);
                gameComplete = IsGameComplete();
                break;
            case R.id.buttonMoveRight:
                MovePushMan(KEY_RIGHT);
                gameComplete = IsGameComplete();
                break;
        }

        if(gameComplete){
            //스테이지가 클리어 되었을때 처리
            OnCompletedGame();
        }
    }

    //스테이지 클리어 체크
    public boolean IsGameComplete(){
        int row = 0, col = 0;
        for(row = 0; row < COUNT_SCREEN_IMAGE_ROW; row ++){
            for(col = 0; col < COUNT_SCREEN_IMAGE_COL; col ++){
                if(mScreenImageType[row][col]==IMAGE_TYPE_STONE){
                    return false;
                }
            }
        }
        //돌구슬이 발견 되지 않으면 True 를 반환
        return true;
    }

    //스테이지가 클리어 되었을때 처리하는 함수.
    public void OnCompletedGame(){
        String messageText = String.format("Conguraturation! You Passed Level=%d", mGameLevelNum);

        //팝업 메시지를 화면에 표시
        new AlertDialog.Builder(this).setMessage(messageText)
                .setTitle("Level Completed!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton){
                        //사용자가 버튼을 누르면 새로운 스테이지 로딩
                        LoadDataFile(mGameLevelNum + 1);
                    }
                }).show();
    }

    //터치 이벤트로 푸쉬맨 이동시키기
    public boolean onTouchEvent(MotionEvent event){
        boolean gameComplete = false;
        super.onTouchEvent(event);

        //TouchDown 이벤트 일때 터치 좌표를 맴버 변수에 저장
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            mPoTouch.x = (int)event.getX();
            mPoTouch.y = (int)event.getY();

            //TouchMove 일 경우에 푸쉬맨을 이동
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if(mPoTouch.x == -1 && mPoTouch.y == -1){
                return true;
            }

            //터치 포인트 이동거리 계산
            int nMoveX = (int)event.getX() - mPoTouch.x;
            int nMoveY = (int)event.getY() - mPoTouch.y;

            //수평방향으로 이동했을 경우
            if(Math.abs(nMoveX) >= LENGTH_IMAGE_WIDTH ){
                //왼쪽으로 이동할경우 푸쉬맨을 왠쪽으로 이동
                if(nMoveX < 0){
                    MovePushMan(KEY_LEFT);
                }else{
                    MovePushMan(KEY_RIGHT);
                }
             //수직 방향으로 이동했을 경우
            }else if(Math.abs(nMoveY) >= LENGTH_IMAGE_HEIGHT){
                //위로 이동했다면 푸쉬맨을 위로 이동
                if(nMoveY < 0){
                    MovePushMan(KEY_UP);
                    //아래쪽으로 움직였다면 아래쪽으로 이동
                }else{
                    MovePushMan(KEY_DOWN);
                }

            }else{
                return true;
            }

            //게임이 클리어 되었는지를 구한다.
            gameComplete = IsGameComplete();
            mPoTouch.x = (int)event.getX();
            mPoTouch.y = (int)event.getY();

            //TouchUP 이벤트일 경우 터치 좌표를 초기화
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            mPoTouch.x = -1;
            mPoTouch.y = -1;
        }

        if(gameComplete){
            mPoTouch.x = -1;
            mPoTouch.y = -1;

            OnCompletedGame();
        }
        return true;
    }


    //커스텀 비프음 재생 함수
    public void playSoundPool(int soundId){
        mSoundPool.play(soundId,1,1,0,0,1);
    }
}


