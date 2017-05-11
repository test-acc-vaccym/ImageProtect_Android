package bying.imageprotect.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;

import java.io.InputStream;

import bying.imageprotect.R;
import bying.imageprotect.base.FaceDetectorBase;

/**
 * Bitmap是Android系统中的图像处理的最重要类之一。
 * 用它可以获取图像文件信息，进行图像剪切、旋转、缩放等操作，
 * 并可以指定格式保存图像文件。
 * OpenCV的彩色是BGR不是RGB.
 * Mat即矩阵（Matrix）的缩写.
 */

public class ShareActivity extends FaceDetectorBase {

    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView nav;

    //图像变量
    private double max_size = 1024;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView image1, image2, image3;
    private Bitmap srcImg = null;
    private Bitmap srcFace = null;
    private Button selectImageBtn,detectFaceBtn;

    private boolean state = false;//判断是否选择了图片

    FaceDetector faceDetector = null;
    FaceDetector.Face[] face;
    final int N_MAX = 5;
//    ProgressBar progressBar = null;
    ProgressDialog progress;

//    Thread checkFaceThread = new Thread(){
//
//        @Override
//        public void run() {
//            // TODO Auto-generated method stub
//            Bitmap faceBitmap = detectFace();
//            mainHandler.sendEmptyMessage(2);
//            Message m = new Message();
//            m.what = 0;
//            m.obj = faceBitmap;
//            mainHandler.sendMessage(m);
//
//        }
//
//    };
//    Handler mainHandler = new Handler(){
//
//        @Override
//        public void handleMessage(Message msg) {
//            // TODO Auto-generated method stub
//            //super.handleMessage(msg);
//            switch (msg.what){
//                case 0:
//                    Bitmap b = (Bitmap) msg.obj;
//                    image1.setImageBitmap(b);
//                    ShowToast("检测完毕");
//                    break;
//                case 1:
//                    progress = new ProgressDialog(ShareActivity.this);
//                    progress.setMessage("正在检测...");
//                    progress.setCanceledOnTouchOutside(false);
//                    progress.show();
////                    showProcessBar();
//                    break;
//                case 2:
////                    progressBar.setVisibility(View.GONE);
//                    progress.dismiss();
////                    detectFaceBtn.setClickable(false);
//                    break;
//                default:
//                    break;
//            }
//        }
//
//    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        toolbar = (Toolbar) findViewById(R.id.tl_custom);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.share_draw);
        nav = (NavigationView) findViewById(R.id.nav_view);
        loginListener(nav);
        navigationListener(nav, 2);
        initToolbar();//注意和侧滑的顺序，否则无法点击
        initLeftSlip(toolbar, mDrawerLayout);
        //        initToolbar();

        //图像处理
        staticLoadCVLibraries();
        image1 = (ImageView) findViewById(R.id.image1);
        //        image2 = (ImageView) findViewById(R.id.image2);
        //        image3 = (ImageView) findViewById(R.id.image3);

        selectImageBtn = (Button) findViewById(R.id.btn_selectImage);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // makeText(MainActivity.this.getApplicationContext(), "start to browser image", Toast.LENGTH_SHORT).show();

                if (ContextCompat.checkSelfPermission(ShareActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ShareActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    ShowToast("打开相册");
                    selectImage();

                }
            }
        });

//        initUI();

        detectFaceBtn = (Button) findViewById(R.id.btn_detectFace);
        detectFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // makeText(MainActivity.this.getApplicationContext(), "hello, image process", Toast.LENGTH_SHORT).show();
                if (!state)
                    ShowToast("请选择照片");
                else {
                    initFaceDetect();
                    progress = new ProgressDialog(ShareActivity.this);
                    progress.setMessage("正在检测...");
                    progress.setCanceledOnTouchOutside(false);
                    progress.show();

                    detectFace();
//                    Bitmap faceBitmap = detectFace();
//                    image1.setImageBitmap(faceBitmap);

//                    mainHandler.sendEmptyMessage(1);
//                    checkFaceThread.start();
                    //                    convertGray();
//                    Utils.matToBitmap(faceDetect(srcImg), srcImg);
//                    image1.setImageBitmap(srcImg);
                }
            }
        });
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.share);//设置Toolbar标题
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white)); //设置标题颜色
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries() {
        boolean load = OpenCVLoader.initDebug();
        if (load) {
            //            Log.i("CV", "Open CV Libraries loaded...");
            ShowLog("Open CV Libraries loaded...");//Base类有logi的封装
        }
    }

    //打开相册选择图片
    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "选择图像..."), PICK_IMAGE_REQUEST);
    }

    //灰度化
//    private void convertGray() {
//        Mat src = new Mat();
//        Mat temp = new Mat();
//        Mat dst = new Mat();
//        Utils.bitmapToMat(bitmap1, src);
//        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//从RGB或BGR图像中删除alpha通道
//        Log.i("CV", "image type:" + (temp.type() == CvType.CV_8UC3));
//        Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);
//        Utils.matToBitmap(dst, bitmap1);
//        image1.setImageBitmap(bitmap1);
//    }

    //访问权限
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectImage();
                } else {
                    ShowToast("你拒绝了访问权限");
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            state = true;
        }
        //        if(requestCode > PICK_IMAGE_REQUEST){
        //            ShowToast("最多选择"+PICK_IMAGE_REQUEST+"张");
        //        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Log.d("image-tag", "start to decode selected image now...");
                InputStream input = getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                int raw_width = options.outWidth;
                int raw_height = options.outHeight;
                int max = Math.max(raw_width, raw_height);
                int newWidth = raw_width;
                int newHeight = raw_height;
                int inSampleSize = 1;
                if (max > max_size) {//压缩图片
                    newWidth = raw_width / 2;
                    newHeight = raw_height / 2;
                    while ((newWidth / inSampleSize) > max_size || (newHeight / inSampleSize) > max_size) {
                        inSampleSize *= 2;
                    }
                }

                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                srcImg = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);

                image1.setImageBitmap(srcImg);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//    public void initUI(){
//
//        detectFaceBtn = (Button)findViewById(R.id.btn_detect_face);
//        LayoutParams params = image1.getLayoutParams();
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        int w_screen = dm.widthPixels;
//        //		int h = dm.heightPixels;
//
//        srcImg = bitmap1;
//        int h = srcImg.getHeight();
//        int w = srcImg.getWidth();
//        float r = (float)h/(float)w;
//        params.width = w_screen;
//        params.height = (int)(params.width * r);
//        image1.setLayoutParams(params);
//        image1.setImageBitmap(srcImg);
//    }

    public void initFaceDetect(){
        srcFace = srcImg.copy(Config.RGB_565, true);
        int w = srcFace.getWidth();
        int h = srcFace.getHeight();
        ShowLog("待检测图像: w = " + w + "h = " + h);
        faceDetector = new FaceDetector(w, h, N_MAX);
        face = new FaceDetector.Face[N_MAX];
    }
    public boolean checkFace(Rect rect){
        int w = rect.width();
        int h = rect.height();
        int s = w*h;
        ShowLog("人脸 宽 w = " + w + ", 高 h = " + h + ", 人脸面积 s = " + s);
//        if(s < 10000){
//            ShowLog("无效人脸，舍弃.");
//            return false;
//        }
//        else{
//            ShowLog("有效人脸，保存.");
            return true;
//        }
    }
    public void detectFace(){
        //		Drawable d = getResources().getDrawable(R.drawable.face_2);
        //		Log.i(tag, "Drawable尺寸 w = " + d.getIntrinsicWidth() + "h = " + d.getIntrinsicHeight());
        //		BitmapDrawable bd = (BitmapDrawable)d;
        //		Bitmap srcFace = bd.getBitmap();

        int nFace = faceDetector.findFaces(srcFace, face);
        if(nFace==0) {
            ShowToast("未检测到人脸");
            return;
        }
        ShowLog("检测到人脸：n = " + nFace);
        for(int i=0; i<nFace; i++){
            Face f  = face[i];
            PointF midPoint = new PointF();
            float dis = f.eyesDistance();
            f.getMidPoint(midPoint);
            int dd = (int)(dis);
            Point eyeLeft = new Point((int)(midPoint.x - dis/2), (int)midPoint.y);
            Point eyeRight = new Point((int)(midPoint.x + dis/2), (int)midPoint.y);
            Rect faceRect = new Rect((int)(midPoint.x - dd), (int)(midPoint.y - dd), (int)(midPoint.x + dd), (int)(midPoint.y + dd));
            ShowLog("左眼坐标 x = " + eyeLeft.x + ", y = " + eyeLeft.y);
            if(checkFace(faceRect)){
                Canvas canvas = new Canvas(srcFace);
                Paint p = new Paint();
                p.setAntiAlias(true);
                p.setStrokeWidth(8);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.GREEN);
//                canvas.drawCircle(eyeLeft.x, eyeLeft.y, 20, p);
//                canvas.drawCircle(eyeRight.x, eyeRight.y, 20, p);
                canvas.drawRect(faceRect, p);
            }

        }
//        ImageUtil.saveJpeg(srcFace);
//        ShowLog("保存完毕");

        //将绘制完成后的faceBitmap返回
        ShowToast("检测完毕");
        progress.dismiss();
//        return srcFace;
//        Bitmap faceBitmap = detectFace();
        image1.setImageBitmap(srcFace);

    }
//    public void showProcessBar(){
//        RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.share_draw);
//        progressBar = new ProgressBar(ShareActivity.this, null, android.R.attr.progressBarStyleLargeInverse); //ViewGroup.LayoutParams.WRAP_CONTENT
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
//        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
//        progressBar.setVisibility(View.VISIBLE);
//        //progressBar.setLayoutParams(params);
//        mainLayout.addView(progressBar, params);
//
//    }


}
