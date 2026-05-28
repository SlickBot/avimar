(function () {
    const map = L.map('map', {
        zoomControl: false,
        attributionControl: true,
    }).setView([45.801, 15.165], 6);

    // ---------- Tile layers ----------
    const tileLayers = {
        cartoDark: L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            maxZoom: 22, attribution: '&copy; OSM &copy; CARTO', subdomains: 'abcd',
        }),
        cartoLight: L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
            maxZoom: 22, attribution: '&copy; OSM &copy; CARTO', subdomains: 'abcd',
        }),
        cartoVoyager: L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            maxZoom: 22, attribution: '&copy; OSM &copy; CARTO', subdomains: 'abcd',
        }),
        osm: L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19, attribution: '&copy; OpenStreetMap',
        }),
        esriSat: L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19, attribution: 'Tiles &copy; Esri',
        }),
    };
    let activeTileLayer = tileLayers.cartoDark;
    activeTileLayer.addTo(map);

    // ---------- Layers (order = z-order, lowest first) ----------
    const trailsLayer = L.layerGroup().addTo(map);
    const aircraftLayer = L.markerClusterGroup({
        maxClusterRadius: 60,
        disableClusteringAtZoom: 10,
        chunkedLoading: true,
        spiderfyOnMaxZoom: false,
        showCoverageOnHover: false,
    }).addTo(map);
    const vesselLayer = L.markerClusterGroup({
        maxClusterRadius: 50,
        disableClusteringAtZoom: 11,
        chunkedLoading: true,
        spiderfyOnMaxZoom: false,
        showCoverageOnHover: false,
    }).addTo(map);

    const aircraftMarkers = new Map();   // hex  -> marker
    const aircraftHistory = new Map();   // hex  -> [[lat,lon], ...]
    const aircraftTrails = new Map();    // hex  -> polyline
    const vesselMarkers = new Map();     // mmsi -> marker
    const vesselHistory = new Map();     // mmsi -> [[lat,lon], ...]
    const vesselTrails = new Map();      // mmsi -> polyline

    const MAX_TRAIL_POINTS = 40;
    let showTrails = true;
    let aviationVisible = true;
    let maritimeVisible = true;
    let selectionRing = null;

    // ---------- Aircraft ----------
    function planeSvg(track, color) {
        const t = Math.round(track || 0);
        return (
            '<div class="ac-icon" style="transform: rotate(' + t + 'deg);">' +
            '<svg width="20" height="20" viewBox="-12 -12 24 24">' +
            '<path d="M0,-10 L2,-3 L10,0 L10,2 L2,1 L1,8 L4,10 L4,11 L0,9.5 L-4,11 L-4,10 L-1,8 L-2,1 L-10,2 L-10,0 L-2,-3 Z" ' +
            'fill="' + color + '" stroke="#101018" stroke-width="0.5" stroke-linejoin="round"/>' +
            '</svg></div>'
        );
    }

    function aircraftColor(a) {
        const alt = a.alt;
        if (alt == null) return '#9e9e9e';
        if (alt < 5000) return '#ff5252';
        if (alt < 15000) return '#ffb300';
        if (alt < 30000) return '#4fc3f7';
        return '#b39ddb';
    }

    function aircraftLabel(a) {
        return a.callsign || a.registration || a.hex.toUpperCase();
    }

    function makeAircraftIcon(a) {
        return L.divIcon({
            className: '',
            html: planeSvg(a.track, aircraftColor(a)) +
                  '<div class="ac-label">' + aircraftLabel(a) + '</div>',
            iconSize: [20, 20],
            iconAnchor: [10, 10],
        });
    }

    function pushHistory(history, id, lat, lon) {
        let hist = history.get(id);
        if (!hist) { hist = []; history.set(id, hist); }
        const last = hist[hist.length - 1];
        if (!last || last[0] !== lat || last[1] !== lon) {
            hist.push([lat, lon]);
            if (hist.length > MAX_TRAIL_POINTS) hist.shift();
        }
        return hist;
    }

    function syncTrail(trailMap, history, id, color) {
        const hist = history.get(id);
        const existing = trailMap.get(id);
        if (!showTrails || !hist || hist.length < 2) {
            if (existing) { trailsLayer.removeLayer(existing); trailMap.delete(id); }
            return;
        }
        if (existing) {
            existing.setLatLngs(hist);
        } else {
            const line = L.polyline(hist, { color: color, weight: 1.5, opacity: 0.55 });
            line.addTo(trailsLayer);
            trailMap.set(id, line);
        }
    }

    // ---------- Vessels ----------
    function shipSvg(heading, color) {
        const t = Math.round(heading || 0);
        return (
            '<div class="ac-icon" style="transform: rotate(' + t + 'deg);">' +
            '<svg width="14" height="20" viewBox="-7 -10 14 20">' +
            '<path d="M0,-9 L5,-2 L5,8 L-5,8 L-5,-2 Z" ' +
            'fill="' + color + '" stroke="#101018" stroke-width="0.6" stroke-linejoin="round"/>' +
            '</svg></div>'
        );
    }

    function vesselColor(v) {
        const t = v.shipType;
        if (t == null) return '#80cbc4';
        if (t >= 60 && t < 70) return '#26a69a';
        if (t >= 70 && t < 80) return '#fbc02d';
        if (t >= 80 && t < 90) return '#ef5350';
        if (t >= 30 && t < 40) return '#ab47bc';
        return '#80cbc4';
    }

    function vesselLabel(v) {
        return v.name || ('MMSI ' + v.mmsi);
    }

    function makeVesselIcon(v) {
        const h = (v.heading != null) ? v.heading : v.cog;
        return L.divIcon({
            className: '',
            html: shipSvg(h, vesselColor(v)) +
                  '<div class="ac-label">' + vesselLabel(v) + '</div>',
            iconSize: [14, 20],
            iconAnchor: [7, 10],
        });
    }

    // ---------- Viewport ----------
    function emitViewport() {
        const c = map.getCenter();
        const b = map.getBounds();
        const ne = b.getNorthEast();
        const sw = b.getSouthWest();
        const meters = c.distanceTo(ne);
        const nm = Math.min(250, meters / 1852);
        if (window.AirnavBridge && window.AirnavBridge.onViewport) {
            window.AirnavBridge.onViewport(
                c.lat, c.lng, nm, map.getZoom(),
                sw.lat, sw.lng, ne.lat, ne.lng
            );
        }
    }
    map.on('moveend', emitViewport);
    map.whenReady(emitViewport);

    // ---------- Public API ----------
    window.airnav = {
        map: map,

        setCenter: function (lat, lon, zoom) {
            if (zoom != null) {
                map.setView([lat, lon], zoom, { animate: true });
            } else {
                map.panTo([lat, lon], { animate: true });
            }
        },

        setTileStyle: function (name) {
            const next = tileLayers[name];
            if (!next || next === activeTileLayer) return;
            map.removeLayer(activeTileLayer);
            next.addTo(map);
            activeTileLayer = next;
        },

        setTrailsEnabled: function (on) {
            showTrails = !!on;
            if (!showTrails) {
                aircraftTrails.forEach(t => trailsLayer.removeLayer(t));
                aircraftTrails.clear();
                vesselTrails.forEach(t => trailsLayer.removeLayer(t));
                vesselTrails.clear();
            } else {
                aircraftHistory.forEach((_h, id) => syncTrail(aircraftTrails, aircraftHistory, id, '#4fc3f7'));
                vesselHistory.forEach((_h, id) => syncTrail(vesselTrails, vesselHistory, id, '#80cbc4'));
            }
        },

        setAviationVisible: function (on) {
            aviationVisible = !!on;
            if (aviationVisible) {
                if (!map.hasLayer(aircraftLayer)) aircraftLayer.addTo(map);
            } else if (map.hasLayer(aircraftLayer)) {
                map.removeLayer(aircraftLayer);
            }
        },

        setSelection: function (kind, lat, lon) {
            const color = (kind === 'ship') ? '#4dd0e1' : '#6ec8ff';
            if (selectionRing) {
                selectionRing.setLatLng([lat, lon]);
                selectionRing.setStyle({ color: color });
            } else {
                selectionRing = L.circleMarker([lat, lon], {
                    radius: 18,
                    color: color,
                    weight: 2,
                    fillOpacity: 0.0,
                    dashArray: '4 4',
                    interactive: false,
                });
                selectionRing.addTo(map);
            }
        },

        clearSelection: function () {
            if (selectionRing) {
                map.removeLayer(selectionRing);
                selectionRing = null;
            }
        },

        setMaritimeVisible: function (on) {
            maritimeVisible = !!on;
            if (maritimeVisible) {
                if (!map.hasLayer(vesselLayer)) vesselLayer.addTo(map);
            } else if (map.hasLayer(vesselLayer)) {
                map.removeLayer(vesselLayer);
            }
        },

        updateAircraft: function (list) {
            const seen = new Set();
            for (let i = 0; i < list.length; i++) {
                const a = list[i];
                seen.add(a.hex);
                pushHistory(aircraftHistory, a.hex, a.lat, a.lon);

                let marker = aircraftMarkers.get(a.hex);
                if (marker) {
                    marker.setLatLng([a.lat, a.lon]);
                    marker.setIcon(makeAircraftIcon(a));
                } else {
                    marker = L.marker([a.lat, a.lon], { icon: makeAircraftIcon(a) });
                    marker.on('click', () => {
                        if (window.AirnavBridge && window.AirnavBridge.onAircraftClick) {
                            window.AirnavBridge.onAircraftClick(a.hex);
                        }
                    });
                    aircraftLayer.addLayer(marker);
                    aircraftMarkers.set(a.hex, marker);
                }
                syncTrail(aircraftTrails, aircraftHistory, a.hex, '#4fc3f7');
            }
            aircraftMarkers.forEach((m, hex) => {
                if (!seen.has(hex)) {
                    aircraftLayer.removeLayer(m);
                    aircraftMarkers.delete(hex);
                    aircraftHistory.delete(hex);
                    const tr = aircraftTrails.get(hex);
                    if (tr) { trailsLayer.removeLayer(tr); aircraftTrails.delete(hex); }
                }
            });
        },

        updateVessels: function (list) {
            const seen = new Set();
            for (let i = 0; i < list.length; i++) {
                const v = list[i];
                seen.add(v.mmsi);
                pushHistory(vesselHistory, v.mmsi, v.lat, v.lon);

                let marker = vesselMarkers.get(v.mmsi);
                if (marker) {
                    marker.setLatLng([v.lat, v.lon]);
                    marker.setIcon(makeVesselIcon(v));
                } else {
                    marker = L.marker([v.lat, v.lon], { icon: makeVesselIcon(v) });
                    marker.on('click', () => {
                        if (window.AirnavBridge && window.AirnavBridge.onVesselClick) {
                            window.AirnavBridge.onVesselClick(v.mmsi);
                        }
                    });
                    vesselLayer.addLayer(marker);
                    vesselMarkers.set(v.mmsi, marker);
                }
                syncTrail(vesselTrails, vesselHistory, v.mmsi, '#80cbc4');
            }
            vesselMarkers.forEach((m, mmsi) => {
                if (!seen.has(mmsi)) {
                    vesselLayer.removeLayer(m);
                    vesselMarkers.delete(mmsi);
                    vesselHistory.delete(mmsi);
                    const tr = vesselTrails.get(mmsi);
                    if (tr) { trailsLayer.removeLayer(tr); vesselTrails.delete(mmsi); }
                }
            });
        },
    };
})();
