package com.example.automatas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class AutomataView extends View {

    static class Node {
        String name;
        float x, y;
        boolean isInitial, isFinal;

        Node(String name, float x, float y, boolean isInitial, boolean isFinal) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.isInitial = isInitial;
            this.isFinal = isFinal;
        }
    }

    static class Transition {
        Node fromNode, toNode;
        String value;

        Transition(Node fromNode, Node toNode, String value) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.value = value;
        }
    }

    private List<Node> nodes = new ArrayList<>();
    private List<Transition> transitions = new ArrayList<>();
    private Paint nodePaint, transitionPaint, textPaint;

    public AutomataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Inicializar el Paint para los nodos
        nodePaint = new Paint();
        nodePaint.setStyle(Paint.Style.STROKE);
        nodePaint.setStrokeWidth(5);
        nodePaint.setColor(Color.RED);

        // Inicializar el Paint para las transiciones
        transitionPaint = new Paint();
        transitionPaint.setColor(Color.GREEN);
        transitionPaint.setStrokeWidth(3);

        // Inicializar el Paint para el texto
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        if (nodes.isEmpty()) return; // No dibujar si no hay nodos

        // Calcular el tamaño total del dibujo
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (Node node : nodes) {
            minX = Math.min(minX, node.x);
            maxX = Math.max(maxX, node.x);
            minY = Math.min(minY, node.y);
            maxY = Math.max(maxY, node.y);
        }
        float drawingWidth = maxX - minX;
        float drawingHeight = maxY - minY;

        // Calcular el desplazamiento para centrar el dibujo
        float offsetX = (getWidth() - drawingWidth) / 2 - minX;
        float offsetY = (getHeight() - drawingHeight) / 2 - minY;
        canvas.translate(offsetX, offsetY);

        // Ajustar el factor de escala automáticamente
        float scaleFactor = Math.min((float) getWidth() / drawingWidth, (float) getHeight() / drawingHeight) * 0.9f;
        canvas.scale(scaleFactor, scaleFactor, drawingWidth / 2 + minX, drawingHeight / 2 + minY);

        // Dibujar transiciones
        for (Transition transition : transitions) {
            drawTransition(canvas, transition);
        }

        // Dibujar nodos
        for (Node node : nodes) {
            drawNode(canvas, node);
        }
    }

    private void drawNode(Canvas canvas, Node node) {
        float radius = 50; // Ajustar tamaño del nodo
        canvas.drawCircle(node.x, node.y, radius, nodePaint);
        canvas.drawText(node.name, node.x, node.y + 20, textPaint);

        // Dibujar un punto central para validar la posición del nodo
        Paint centerPaint = new Paint();
        centerPaint.setColor(Color.YELLOW);
        canvas.drawCircle(node.x, node.y, 5, centerPaint); // Pinta un punto central
    }



    private void drawTransition(Canvas canvas, Transition transition) {
        canvas.drawLine(transition.fromNode.x, transition.fromNode.y, transition.toNode.x, transition.toNode.y, transitionPaint);

        // Dibuja una flecha en el extremo final de la línea
        float angle = (float) Math.atan2(transition.toNode.y - transition.fromNode.y, transition.toNode.x - transition.fromNode.x);
        float arrowSize = 6; // Ajusta este valor según el tamaño deseado

        float arrowX1 = transition.toNode.x - arrowSize * (float) Math.cos(angle - Math.PI / 6);
        float arrowY1 = transition.toNode.y - arrowSize * (float) Math.sin(angle - Math.PI / 6);
        float arrowX2 = transition.toNode.x - arrowSize * (float) Math.cos(angle + Math.PI / 6);
        float arrowY2 = transition.toNode.y - arrowSize * (float) Math.sin(angle + Math.PI / 6);
        canvas.drawLine(transition.toNode.x, transition.toNode.y, arrowX1, arrowY1, transitionPaint);
        canvas.drawLine(transition.toNode.x, transition.toNode.y, arrowX2, arrowY2, transitionPaint);
    }


    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        invalidate();
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
        invalidate();
    }
}

