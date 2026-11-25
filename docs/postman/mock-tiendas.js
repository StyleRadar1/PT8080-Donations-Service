/**
 * Mock ligero de ms-tiendas (puerto 8140) para ejecutar de forma autónoma
 * toda la colección Postman / Newman en perfil local, incluyendo casos 200, 400, 404 y 503.
 *
 * Uso: node docs/postman/mock-tiendas.js
 */
const http = require("http");

let failNextListCount = 0;

const foundations = [
  {
    id: 3,
    nombre: "Fundación Abrigo Solidario",
    barrio: "Chapinero",
    descripcion: "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá.",
    direccion: "Calle 85 # 15-32, Chapinero, Bogotá",
    telefono: "+57 601 742 8899",
    email: "contacto@abrigosolidario.org",
    categoriasAceptadas: ["SUPERIOR", "INFERIOR", "CALZADO", "ACCESORIOS"],
    causasApoyadas: "Niñez vulnerable y adultos mayores en Bogotá",
    horarioAtencion: "Lunes a Sábado de 08:00 a 17:00",
    politicasDonacion: "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo",
    activa: true,
    representanteId: 501
  },
  {
    id: 99,
    nombre: "Fundación Inactiva Demo",
    barrio: "Suba",
    descripcion: "Fundación temporalmente suspendida para pruebas de validación HTTP 400.",
    direccion: "Av. Suba # 115-20, Bogotá",
    telefono: "+57 601 700 0099",
    email: "inactiva@styleradar.local",
    categoriasAceptadas: ["SUPERIOR"],
    causasApoyadas: "Pruebas de validación",
    horarioAtencion: "Cerrada temporalmente",
    politicasDonacion: "No recibe donaciones actualmente",
    activa: false,
    representanteId: 998
  }
];

function sendJson(res, status, payload) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, "http://localhost:8140");

  if (req.method === "POST" && url.pathname === "/__mock/fail-next-list") {
    failNextListCount = 1;
    return sendJson(res, 200, { mode: "fail-next-list", remaining: failNextListCount });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/admin/foundations") {
    if (failNextListCount > 0) {
      failNextListCount -= 1;
      return sendJson(res, 503, { error: "Simulated ms-tiendas unavailability (503)" });
    }
    return sendJson(res, 200, foundations);
  }

  const match = url.pathname.match(/^\/api\/v1\/admin\/foundations\/(\d+)$/);
  if (req.method === "GET" && match) {
    const id = Number(match[1]);
    if (id === 503) {
      return sendJson(res, 503, { error: "Simulated ms-tiendas 503 for foundation 503" });
    }
    const found = foundations.find((f) => f.id === id);
    if (!found) {
      return sendJson(res, 404, { error: "Foundation not found", id });
    }
    return sendJson(res, 200, found);
  }

  return sendJson(res, 404, { error: "Route not mocked", path: url.pathname });
});

const PORT = Number(process.env.MOCK_TIENDAS_PORT || 8140);
server.listen(PORT, () => {
  console.log(`[mock-tiendas] Escuchando en http://localhost:${PORT}`);
});
