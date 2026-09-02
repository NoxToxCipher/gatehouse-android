# Gatehouse Operations — Master Engineering & API Roadmap
**Doherty Security Services (DSS)** · Hume Doors & Timber (Kingston, QLD)

---

## 🟢 1. Live & Deployed Modules

| # | Feature / Module | Architecture & APIs | Operational Focus | Status |
|---|---|---|---|---|
| **1** | **Automatic Number Plate Recognition (ANPR) & Rego** | Camera OCR / QLD Rego check | Heavy vehicle intake, plate logging, blacklist alerts, PDF attachment | ✅ **Complete & Live** |
| **2** | **Encrypted Push-to-Talk (PTT) Digital Radio** | Local UDP Multicast (`239.255.41.207`), Foreground Service | Sub-100ms 2-way voice, Roger beeps, screen-off pocket audio & 15s replay | ✅ **Complete & Live** |
| **3** | **Emergency Services, Bushfire & BOM Radar** | QFES Live GeoJSON / Open-Meteo FWI / AFDRS | 10km perimeter radar sweep, ember vector math & shift weather alerts | ✅ **Complete & Live** |
| **4** | **Security Licence Verification & Expiry Reminders** | QLD Fair Trading / SPARK Attestation | 3-month, 1-month, 1-fortnight & day-of renewal alerts on Officer Card | ✅ **Complete & Live** |
| **5** | **⚡ Real-Time Lightning Strike Radar & Stand-Down Engine** | Open-Meteo Flash Density / Proximity Calibrator | Configurable distance (<3–10km) & cluster quantity thresholds, WHS stand-down notifications | ✅ **Complete & Live** |
| **6** | **🚁 Low-Altitude Drone & POLAIR Airspace Radar** | ADS-B Transponder API / OpenSky Network | Dual-mode radar HUD, QPS POLAIR 1/2 response orbit detection, 1-tap drone sighting ledger | ✅ **Complete & Live** |
| **7** | **🧊 Severe Thunderstorm & Hail Warning Radar** | Open-Meteo MUCAPE / Convective Precip API | Atmospheric convective instability & hail size estimation (~15–55mm), vehicle cover alerts | ✅ **Complete & Live** |
| **8** | **🌊 Next-Gen Peek & Flow Calendar & Deputy Deck** | Deputy API / Security Award MA000115 / Fluid Physics | 2-Guard overlap matrix, Award penalty calculations, fatigue pacers & 1-tap open shift claiming | ✅ **Complete & Live** |
| **9** | **📚 Site Post Orders, Deputy Docs & Compliance Reader** | Deputy API (`/resource/NewsPost`) / Cryptographic Attestation | Category filter pills, full-text dark reader, font scaler (`A-`/`A+`), 1-tap shift ledger attestation | ✅ **Complete & Live** |

---

## 🌐 2. External API & Cloud Integrations

| # | Feature / Idea | Target API(s) | Operational Value & Workflow |
|---|---|---|---|
| **10** | **📦 Courier Parcel & Freight Consignment Scanner** | AusPost Shipping API / 17Track REST API | Camera scans barcode on parcel -> queries API for recipient/contents -> logs intake & triggers arrival SMS to Hume staff. |
| **11** | **🚦 Inbound Freight Corridor & QLD Traffic Delays** | QLD TMR (`qldtraffic.qld.gov.au`) & TomTom Traffic API | Live congestion/accident alerts on Kingston Rd, Logan Mwy (M2), and M1 for incoming timber trucks. |
| **10** | **🤖 Multimodal Security AI (Gemini 1.5 Flash)** | Google Gemini Flash API (`generativelanguage.googleapis.com`) | **A:** AI visual damage assessment from photos (writes executive report paragraphs).<br>**B:** Real-time driver voice translator (English ↔ Hindi, Punjabi, Vietnamese, Mandarin). |
| **10** | **📱 1-Tap Manager SMS & Emergency Alert Dispatch** | Twilio REST API / MessageMedia | 1-tap instant SMS to Lochran & Hume facility manager (*"Water booster leak Lot 16"*) + emergency distress SMS for lone workers. |
| **11** | **🏢 Contractor ABN & GST Verification Lookup** | Australian Business Register (ABR XML/JSON API) | Instant verification of contractor legal trading name, GST status, and public liability entity without leaving the app. |
| **12** | **🛰️ NASA FIRMS Thermal Satellite Hotspot Scan** | NASA FIRMS (VIIRS/MODIS Satellite API) | Twice-daily thermal infrared satellite scan detecting industrial heat anomalies within 15km of Kingston. |
| **13** | **📧 Automated 06:05 AM Morning Handover Email** | Postmark / Resend REST API | Auto-seals the cryptographic SHA-256 logbook and emails the PDF report to Hume management and HQ at shift end. |

---

## 🏭 3. Industrial Yard, Logistics & Facility Operations

| # | Feature / Idea | Implementation | Operational Value & Workflow |
|---|---|---|---|
| **14** | **🎛️ Analog Needle Gauge Computer Vision Reader** | On-Device OpenCV / Edge ML | Guard aims camera at 1,200 PSI booster gauge -> CV reads needle angle -> extracts exact PSI and logs to fire check in 2 seconds. |
| **15** | **🏷️ NFC / BLE "Smart Patrol Touchpoint" Checkpoints** | Android NFC Reader / BLE Scanner | Guard taps phone to $1 waterproof NFC tags at key perimeter spots (Lot 14 East Gate, Lot 16 Pump Room, Lot 18 Kiln) to verify patrol rounds. |
| **16** | **📋 Delivery Driver 1-Minute Digital WHS Induction** | Interactive Tablet Modal | Truck drivers tap their name, rego, company, review Hume safety rules, and sign on the screen (with multi-language support). |
| **17** | **🗺️ Interactive Yard Bay & Staging Map** | Custom Canvas Grid View | Top-down visual map of Lots 14–18 showing active loading bays, kiln staging, and truck turning paths (`Clear` / `Unloading` / `Blocked`). |
| **18** | **🔑 Site Master Key & Asset Checkout Ledger** | Barcode / QR Scanner + Signature | Digital sign-out ledger for master keys, gate remotes, forklift lock-out tags, and contractor visitor fobs. |
| **19** | **⚖️ NHVR Heavy Vehicle Axle & Curfew Quick Reference** | Local Regulatory Database | Quick lookup for QLD bridge formulas, axle mass limits, and Logan industrial zone oversize vehicle curfew hours. |

---

## 🛡️ 4. Guard Welfare, Lone Worker & Night Operations

| # | Feature / Idea | Implementation | Operational Value & Workflow |
|---|---|---|---|
| **20** | **⏳ Motion-Sensing Deadman's Welfare Switch** | Accelerometer / Gyro Step Detector | Auto-detects rover movement at night; plays gold chime after 30 mins of inactivity; dispatches distress alert if unacknowledged. |
| **21** | **🚨 1-Tap Perimeter Yard Strobe & Deterrence Siren** | Camera Flash Strobe (15Hz) + Audio Synth | High-frequency strobe + loud siren to disorient intruders and signal active security engagement. |
| **22** | **🌡️ QLD Wet-Bulb Heat Stress & Hydration Adviser** | BOM Solar/Thermal Calc Engine | Computes occupational heat stress index during humid summer shifts, giving WHS-compliant hydration and rest prompts. |
| **23** | **🌙 Circadian Fatigue Dip Curve & Alertness Tracker** | Shift Bio-Chronograph | Tracks 03:00–04:30 AM night fatigue dip, suggesting alertness lighting shifts and safe-driving advice for the morning drive home. |

---

## 📄 5. Executive Evidence, Incident & Handover Tools

| # | Feature / Idea | Implementation | Operational Value & Workflow |
|---|---|---|---|
| **24** | **✏️ Interactive Incident Diagram & Collision Sketcher** | Vector Drawing Canvas | Guards sketch top-down incident diagrams (forklift bumps, gate scrapes, fence damage) with vehicle stamps; embeds directly in Handover PDF. |
| **25** | **🏷️ Digital Evidence Bag Custody Sealer (SHA-256)** | Cryptographic Tag Generator | Generates tamper-evident custody labels and QR codes for found property or contraband handed to Police/Management. |
| **26** | **🎙️ High-Clarity Voice Interview Recorder** | AudioRecord with High-Pass Filter | Statement recorder with active engine noise filtering for clear driver/contractor statements near running forklifts and trucks. |
| **27** | **🔔 Gate B QR Intercom & Remote Doorbell Chime** | Webhook / Local Socket Trigger | Drivers at unmanned Gate B scan a weatherproof QR code, sounding an executive chime on the Gatehouse Tab S8 base station. |
