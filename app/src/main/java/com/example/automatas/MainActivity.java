package com.example.automatas;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private AutomataView automataView;
    private ImageView imageView;
    private Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        automataView = findViewById(R.id.automataView);
        imageView = findViewById(R.id.imageView);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Error al cargar OpenCV", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar permisos en tiempo de ejecución
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        }

        // Botón para tomar una foto
        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    File photoFile = createImageFile();
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No se encontró una aplicación de cámara compatible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null || !storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio de almacenamiento");
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);

                // Mostrar la imagen en el ImageView
                imageView.setImageBitmap(imageBitmap);

                // Procesar la imagen para detectar y dibujar el autómata
                detectAndDrawAutomata(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar la imagen capturada", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se tomó ninguna foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void detectAndDrawAutomata(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        // Convertir a escala de grises
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        // Aplicar un suavizado para reducir ruido
        Imgproc.GaussianBlur(mat, mat, new Size(9, 9), 2);

        // Operación morfológica para reducir ruido
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_OPEN, kernel);

        // Binarizar usando un umbral adaptativo
        Mat binaryMat = new Mat();
        Imgproc.adaptiveThreshold(mat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2);

        // Detectar bordes usando Canny
        Mat edges = new Mat();
        Imgproc.Canny(binaryMat, edges, 50, 150);

        // Detectar nodos y transiciones
        ArrayList<AutomataView.Node> detectedNodes = detectNodes(edges);
        ArrayList<AutomataView.Transition> detectedTransitions = detectTransitions(edges, detectedNodes);

        automataView.setNodes(detectedNodes);
        automataView.setTransitions(detectedTransitions);
    }

    private ArrayList<AutomataView.Node> detectNodes(Mat edges) {
        ArrayList<AutomataView.Node> nodes = new ArrayList<>();
        Mat circles = new Mat();

        // Obtener dimensiones de la imagen
        int imageHeight = edges.rows();
        int imageWidth = edges.cols();

        // Ajuste dinámico del parámetro `minDist`
        double minDist = Math.min(imageWidth, imageHeight) * 0.2;

        // Detectar círculos con HoughCircles
        Imgproc.HoughCircles(edges, circles, Imgproc.HOUGH_GRADIENT,
                1.5, minDist, // Ajuste dinámico de la distancia mínima
                100, 40, // Param1 y Param2 ajustados
                30, 70); // MinRadius y MaxRadius ajustados al tamaño esperado

        for (int i = 0; i < circles.cols(); i++) {
            double[] circleData = circles.get(0, i);
            if (circleData == null) continue;

            float x = (float) circleData[0];
            float y = (float) circleData[1];
            float radius = (float) circleData[2];

            // Filtro adicional para excluir círculos con radios extremos
            if (radius < 20 || radius > 70) continue;

            // Verificar si ya existe un nodo cerca para evitar duplicados
            boolean isDuplicate = false;
            for (AutomataView.Node node : nodes) {
                float distance = (float) Math.hypot(node.x - x, node.y - y);
                if (distance < radius * 2.5) { // Ajuste dinámico basado en el radio
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                // Agregar nodo único
                String nodeName = "q" + nodes.size();
                nodes.add(new AutomataView.Node(nodeName, x, y, false, false));
            }
        }

        return nodes;
    }

    private ArrayList<com.example.automatas.AutomataView.Transition> detectTransitions(Mat edges, ArrayList<AutomataView.Node> nodes) {
        ArrayList<AutomataView.Transition> transitions = new ArrayList<>();
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 50, 50, 10);

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            float x1 = (float) line[0];
            float y1 = (float) line[1];
            float x2 = (float) line[2];
            float y2 = (float) line[3];

            AutomataView.Node fromNode = findClosestNode(x1, y1, nodes);
            AutomataView.Node toNode = findClosestNode(x2, y2, nodes);

            // Validar que las líneas no conecten nodos iguales
            if (fromNode != null && toNode != null && fromNode != toNode) {
                transitions.add(new AutomataView.Transition(fromNode, toNode, "a"));
            }
        }
        return transitions;
    }

    private AutomataView.Node findClosestNode(float x, float y, ArrayList<AutomataView.Node> nodes) {
        AutomataView.Node closestNode = null;
        float minDistance = Float.MAX_VALUE;

        for (com.example.automatas.AutomataView.Node node : nodes) {
            float distance = (float) Math.hypot(node.x - x, node.y - y);
            if (distance < minDistance) {
                minDistance = distance;
                closestNode = node;
            }
        }
        return closestNode;
    }
}
