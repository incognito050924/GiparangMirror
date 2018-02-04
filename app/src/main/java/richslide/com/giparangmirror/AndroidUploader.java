package richslide.com.giparangmirror;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

import richslide.com.giparangmirror.common.GiparangConst;
import richslide.com.giparangmirror.impl.IUploader;

/**
 * Created by 장윤석 on 2016-05-18.
 */
public class AndroidUploader {

    /*static String serviceDomain = "http://192.168.2.3:8080/menuSignageService";
    static String postUrl = serviceDomain + "/manager/doCreateContent";
    static String CRLF = "\r\n";
    static String twoHyphens = "--";
    static String boundary = "*****b*o*u*n*d*a*r*y*****";*/

    static String serviceDomain = GiparangConst.SERVER_URL;
    static String postUrl = serviceDomain + GiparangConst.SEND_IMAGE;
    static String CRLF = "\r\n";
    static String twoHyphens = "--";
    static String boundary = "*****b*o*u*n*d*a*r*y*****";
    private Context mContext;

    private String pictureFileName = null;
    private DataOutputStream dataStream = null;

    private IUploader uploader;

    public enum ReturnCode { noPicture, unknown, http201, http400, http401, http403, http404, http500};

    private String TAG = "멀티파트";

    public AndroidUploader(Context context) {
        mContext = context;
    }

    public static void setServiceDomain(String domainName)        {
        serviceDomain = domainName;
    }

    public static String getServiceDomain()        {
        return serviceDomain;
    }

    public JSONObject uploadPicture(String pictureFileName, IUploader uploader)        {
        JSONObject jsonObject = null;
        this.uploader = uploader;
        try {
            jsonObject =  new ProcessFacebookTask().execute(pictureFileName,null,null).get();
            //code = new MulipartUploadTask().execute(pictureFileName,null,null).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String getResponse(HttpURLConnection conn)        {
        try             {
            DataInputStream dis = new DataInputStream(conn.getInputStream());
            byte []        data = new byte[1024];
            int             len = dis.read(data, 0, 1024);

            dis.close();
            int responseCode = conn.getResponseCode();

            if (len > 0)
                return new String(data, 0, len);
            else
                return "";
        }
        catch(Exception e)     {
            //System.out.println("AndroidUploader: "+e);
            Log.e(TAG, "AndroidUploader: "+e);
            return "";
        }
    }

    /**
     *  this mode of reading response no good either
     */
    private String getResponseOrig(HttpURLConnection conn)        {
        InputStream is = null;
        try   {
            is = conn.getInputStream();
            // scoop up the reply from the server
            int ch;
            StringBuffer sb = new StringBuffer();
            while( ( ch = is.read() ) != -1 ) {
                sb.append( (char)ch );
            }
            return sb.toString();   // TODO Auto-generated method stub
        }
        catch(Exception e)   {
            //System.out.println("GeoPictureUploader: biffed it getting HTTPResponse");
            Log.e(TAG, "AndroidUploader: "+e);
        }
        finally   {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {}
        }

        return "";
    }

    /**
     * write one form field to dataSream
     * @param fieldName
     * @param fieldValue
     */
    private void writeFormField(String fieldName, String fieldValue)  {
        try  {
            dataStream.writeBytes(twoHyphens + boundary + CRLF);
            dataStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"" + CRLF);
            dataStream.writeBytes(CRLF);
            dataStream.writeBytes(URLEncoder.encode(fieldValue, "UTF-8"));
            dataStream.writeBytes(CRLF);
        }    catch(Exception e)   {
            //System.out.println("AndroidUploader.writeFormField: got: " + e.getMessage());
            Log.e(TAG, "AndroidUploader.writeFormField: " + e.getMessage());
        }
    }

    /**
     * write one file field to dataSream
     * @param fieldName - name of file field
     * @param fieldValue - file name
     * @param type - mime type
     */
    private void writeFileField(
            String fieldName,
            String fieldValue,
            String type,
            FileInputStream fis)  {
        try {
            // opening boundary line
            dataStream.writeBytes(twoHyphens + boundary + CRLF);
            dataStream.writeBytes("Content-Disposition: form-data; name=\""
                    + "image"
                    + "\";filename=\""
                    + URLEncoder.encode(fieldValue, "UTF-8")
                    + "\""
                    + CRLF);
            dataStream.writeBytes("Content-Type: " + type +  CRLF);
            dataStream.writeBytes(CRLF);

            // create a buffer of maximum size
            int bytesAvailable = fis.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            // read file and write it into form...
            int bytesRead = fis.read(buffer, 0, bufferSize);
            while (bytesRead > 0)   {
                dataStream.write(buffer, 0, bufferSize);
                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fis.read(buffer, 0, bufferSize);
            }

            // closing CRLF
            dataStream.writeBytes(CRLF);
        }
        catch(Exception e)  {
            //System.out.println("GeoPictureUploader.writeFormField: got: " + e.getMessage());
            Log.e(TAG, "AndroidUploader.writeFormField: got: " + e.getMessage());
        }
    }

    //AsyncTask<Params,Progress,Result>
    private class ProcessFacebookTask extends AsyncTask<String, Void, JSONObject> {

        String fileName=null;

        @Override
        protected JSONObject doInBackground(String... params) {
            String pictureFileName = params[0];
            fileName = pictureFileName;
            File uploadFile = new File(pictureFileName);
            int responseCode = 200;
            if (uploadFile.exists())
                try     {
                    FileInputStream fileInputStream = new FileInputStream(uploadFile);
                    URL connectURL = new URL(postUrl);
                    HttpURLConnection conn = (HttpURLConnection)connectURL.openConnection();

                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");

                    //conn.setRequestProperty("User-Agent", "myFileUploader");
                    conn.setRequestProperty("Connection","Keep-Alive");
                    conn.setRequestProperty("Content-Type","multipart/form-data;boundary="+boundary+"; charset=utf-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");

                    //conn.connect();

                    dataStream = new DataOutputStream(conn.getOutputStream());

                    writeFileField("image", pictureFileName, "image/jpeg", fileInputStream);

                    // final closing boundary line
                    dataStream.writeBytes(twoHyphens + boundary + twoHyphens + CRLF);

                    fileInputStream.close();
                    dataStream.flush();
                    dataStream.close();
                    dataStream = null;
                    conn.connect();

                    Log.d("업로드 테스트", "***********전송완료***********");

                    String response = getResponse(conn);
                    responseCode = conn.getResponseCode();

                    //upload(pictureFileName);


                    if (responseCode == 200 || responseCode == 201) {
                        JSONObject jsonObject = new JSONObject(response);
                        return jsonObject;
                    }
                    else
                        // for now assume bad name/password
                        return null;
                }
                catch (MalformedURLException mue) {
                    Log.e(TAG, "error: " + mue.getMessage(), mue);
                    return null;
                }
                catch (IOException ioe) {
                    Log.e(TAG, "error: " + ioe.getMessage(), ioe);
                    return null;
                }
                catch (Exception e) {
                    Log.e(TAG, "error: " + e.getMessage(), e);
                    return null;
                }    else    {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject code){
            Log.d("RETURN",code+"");
            uploader.loadComplete(code);
        }
    }

    public static void upload(String filePath) throws IOException {
        /*
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();


       // DefaultHttpClient httpclient = new DefaultHttpClient();

        //MultipartEntityBuilder를 다음과 같이 선언
        MultipartEntityBuilder meb = MultipartEntityBuilder.create();

        //Builder 설정하기.
        //선언할때 넣는게 아니라 선언 후 메소드로 설정한다.
        meb.setBoundary(boundary);
        meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        meb.setCharset(Charset.forName("UTF-8"));

        //문자열을 보내려면 addPart와 StringBody가 아닌 addTextBody를 사용한다.
        HttpPostHC4 post = new HttpPostHC4(postUrl);
        meb.addTextBody("config", "1");
        meb.addTextBody("password", "2");
        meb.addBinaryBody("content_file",new File(filePath));

        //HttpEntity를 빌드하고 HttpPost 객체에 삽입한다.
        HttpEntity entity = meb.build();
        post.setEntity(entity);

        CloseableHttpClient httpClient  =  httpClientBuilder.build();
        HttpHost host = new HttpHost(postUrl);
        post.

        httpClient.

        //10초 응답시간 타임아웃 설정
        HttpParams params = httpclient.getParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);

        //org.apache.http.client.HttpClient 로 실행
        HttpClient httpClient = httpclient.build();
        httpClient.
        httpclient.execute(post);
        */
    }


    /**
     * Map 형식으로 Key와 Value를 셋팅한다.
     * @param key : 서버에서 사용할 변수명
     * @param value : 변수명에 해당하는 실제 값
     * @return
     */
    public static String setValue(String key, String value) {
        return "Content-Disposition: form-data; name=\"" + key + "\"r\n\r\n"
                + value;
    }

    /**
     * 업로드할 파일에 대한 메타 데이터를 설정한다.
     * @param key : 서버에서 사용할 파일 변수명
     * @param fileName : 서버에서 저장될 파일명
     * @return
     */
    public static String setFile(String key, String fileName) {
        return "Content-Disposition: form-data; name=\"" + key
                + "\";filename=\"" + fileName + "\"\r\n";
    }
}
