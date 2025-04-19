-- Creación de tabla para el historial de llamadas
CREATE TABLE IF NOT EXISTS call_history (
    id UUID PRIMARY KEY,                         -- Identificador único para cada registro
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL, -- Fecha y hora exacta de la llamada (con zona horaria)
    endpoint_invoked VARCHAR(255) NOT NULL,      -- El path del endpoint que se llamó
    parameters_received TEXT,                    -- Parámetros almacenado recibidos
    response_body TEXT,                          -- Cuerpo de la respuesta (o mensaje de error), almacenado como texto
    http_status INTEGER NOT NULL,                -- Código de estado HTTP de la respuesta
    is_success BOOLEAN NOT NULL,                 -- Indicador simple de si la llamada fue exitosa (status 2xx)
    error_message TEXT                           -- Mensaje de error específico si is_success es false
);