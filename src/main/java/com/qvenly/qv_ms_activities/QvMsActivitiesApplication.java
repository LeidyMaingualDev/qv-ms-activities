package com.qvenly.qv_ms_activities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio qv-ms-activities.
 * Gestiona actividades, asignación de miembros,
 * inscripción de asistentes y control de asistencia QR.
 * Módulos 9, 10, 11, 13 y 14 de los RF.
 */
@SpringBootApplication
public class QvMsActivitiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(QvMsActivitiesApplication.class, args);
    }
}
