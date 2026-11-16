// Estilo del mapa base para tiles generados por Planetiler (esquema OpenMapTiles).
// Todo es local: los tiles salen del .pmtiles y las letras de public/fonts/.
// Orden: fondo -> vegetación -> agua -> edificios -> calles -> nombres.

const FUENTE = ['Noto Sans Regular']

export function crearEstilo(urlTiles) {
  return {
    version: 8,
    glyphs: `${window.location.origin}/fonts/{fontstack}/{range}.pbf`,
    sources: {
      base: {
        type: 'vector',
        url: urlTiles,
        attribution: '© OpenStreetMap contributors',
      },
    },
    layers: [
      { id: 'fondo', type: 'background', paint: { 'background-color': '#f2efe9' } },
      {
        id: 'uso-suelo',
        type: 'fill',
        source: 'base',
        'source-layer': 'landcover',
        paint: { 'fill-color': '#d8e8c8', 'fill-opacity': 0.6 },
      },
      {
        id: 'parques',
        type: 'fill',
        source: 'base',
        'source-layer': 'park',
        paint: { 'fill-color': '#c8e6b0' },
      },
      {
        id: 'agua',
        type: 'fill',
        source: 'base',
        'source-layer': 'water',
        paint: { 'fill-color': '#a8d0f0' },
      },
      {
        id: 'rios',
        type: 'line',
        source: 'base',
        'source-layer': 'waterway',
        paint: { 'line-color': '#a8d0f0', 'line-width': 2 },
      },
      {
        id: 'edificios',
        type: 'fill',
        source: 'base',
        'source-layer': 'building',
        minzoom: 14,
        paint: { 'fill-color': '#ddd6cc', 'fill-outline-color': '#c9c0b4' },
      },
      {
        id: 'calles-borde',
        type: 'line',
        source: 'base',
        'source-layer': 'transportation',
        filter: ['in', ['get', 'class'], ['literal', ['motorway', 'trunk', 'primary', 'secondary', 'tertiary', 'minor', 'service']]],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': '#bbb',
          'line-width': ['interpolate', ['linear'], ['zoom'], 12, 1, 16, 10],
        },
      },
      {
        id: 'calles',
        type: 'line',
        source: 'base',
        'source-layer': 'transportation',
        filter: ['in', ['get', 'class'], ['literal', ['motorway', 'trunk', 'primary', 'secondary', 'tertiary', 'minor', 'service']]],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': [
            'match', ['get', 'class'],
            ['motorway', 'trunk', 'primary'], '#f7c96b',
            'secondary', '#fbe39a',
            '#ffffff',
          ],
          'line-width': ['interpolate', ['linear'], ['zoom'], 12, 0.5, 16, 8],
        },
      },
      {
        id: 'nombres-calles',
        type: 'symbol',
        source: 'base',
        'source-layer': 'transportation_name',
        minzoom: 14,
        layout: {
          'symbol-placement': 'line',
          'text-field': ['get', 'name'],
          'text-font': FUENTE,
          'text-size': 12,
        },
        paint: { 'text-color': '#555', 'text-halo-color': '#fff', 'text-halo-width': 1.5 },
      },
      {
        id: 'lugares',
        type: 'symbol',
        source: 'base',
        'source-layer': 'place',
        layout: {
          'text-field': ['get', 'name'],
          'text-font': FUENTE,
          'text-size': ['match', ['get', 'class'], 'city', 16, 'town', 14, 12],
        },
        paint: { 'text-color': '#333', 'text-halo-color': '#fff', 'text-halo-width': 2 },
      },
    ],
  }
}
