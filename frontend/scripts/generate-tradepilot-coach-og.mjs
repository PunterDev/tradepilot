import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..");
const assetsDirectory = path.join(projectRoot, "public", "assets");
const logoPath = path.join(assetsDirectory, "tradepilot-coach-logo.png");
const outputPath = path.join(assetsDirectory, "tradepilot-coach-og.png");

const logoData = await readFile(logoPath, "base64");
const browser = await chromium.launch({ headless: true });

try {
  const page = await browser.newPage({
    viewport: { width: 1200, height: 630 },
    deviceScaleFactor: 1,
  });

  await page.setContent(
    `<!doctype html>
    <html lang="en">
      <head>
        <style>
          * { box-sizing: border-box; }
          html, body { margin: 0; width: 1200px; height: 630px; overflow: hidden; }
          body {
            background: #040b16;
            color: #f6f2ea;
            font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          }
          .preview {
            position: relative;
            display: flex;
            width: 1200px;
            height: 630px;
            overflow: hidden;
            padding: 56px 62px;
            background:
              linear-gradient(118deg, #071a2b 0%, #040c18 52%, #030914 100%);
          }
          .preview::before {
            position: absolute;
            inset: 0;
            content: "";
            opacity: .32;
            background-image: linear-gradient(rgba(65, 213, 194, .06) 1px, transparent 1px), linear-gradient(90deg, rgba(65, 213, 194, .06) 1px, transparent 1px);
            background-size: 48px 48px;
            mask-image: linear-gradient(90deg, #000, transparent 64%);
          }
          .copy {
            position: relative;
            z-index: 1;
            width: 660px;
            padding-top: 7px;
          }
          .logo {
            display: block;
            width: 322px;
            height: auto;
            margin: 0 0 45px -11px;
            mix-blend-mode: screen;
          }
          .eyebrow {
            margin: 0 0 16px;
            color: #65e6d6;
            font-size: 13px;
            font-weight: 700;
            letter-spacing: 1.3px;
          }
          h1 {
            max-width: 650px;
            margin: 0;
            color: #f8f4ed;
            font-size: 43px;
            font-weight: 650;
            line-height: 1.1;
            letter-spacing: 0;
          }
          h1 strong { color: #75ead5; font-weight: 650; }
          .panel {
            position: absolute;
            z-index: 1;
            top: 57px;
            right: 58px;
            width: 366px;
            min-height: 508px;
            padding: 29px;
            border: 1px solid rgba(67, 211, 194, .38);
            border-radius: 8px;
            background: #081525;
            box-shadow: 0 20px 52px rgba(0, 0, 0, .36), inset 0 1px rgba(161, 255, 240, .05);
          }
          .panel-kicker {
            display: flex;
            align-items: center;
            gap: 8px;
            margin: 0 0 17px;
            color: #74e7d8;
            font-size: 12px;
            font-weight: 750;
            letter-spacing: .9px;
            text-transform: uppercase;
          }
          .signal { width: 8px; height: 8px; border-radius: 50%; background: #5de3ce; box-shadow: 0 0 14px rgba(93, 227, 206, .75); }
          h2 { margin: 0 0 15px; color: #f7f2e9; font-size: 28px; font-weight: 650; line-height: 1.14; letter-spacing: 0; }
          .summary { margin: 0 0 27px; color: #c1d0d8; font-size: 16px; line-height: 1.45; }
          .metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 25px; }
          .metric { min-height: 91px; padding: 14px; border: 1px solid rgba(99, 186, 183, .2); border-radius: 6px; background: #0b1c2d; }
          .metric-label, .focus-label { display: block; color: #8fa6b1; font-size: 11px; font-weight: 700; letter-spacing: .7px; text-transform: uppercase; }
          .metric-value { display: block; margin-top: 10px; color: #f7f2e9; font-size: 24px; font-weight: 700; letter-spacing: 0; }
          .loss { color: #ffb59b; }
          .discipline { color: #74e7d8; }
          .focus { padding-top: 20px; border-top: 1px solid rgba(116, 231, 216, .24); }
          .focus p { margin: 9px 0 0; color: #e6eee9; font-size: 16px; font-weight: 570; line-height: 1.42; }
        </style>
      </head>
      <body>
        <main class="preview">
          <section class="copy">
            <img class="logo" src="data:image/png;base64,${logoData}" alt="TradePilot Coach" />
            <p class="eyebrow">AI EXECUTION INTELLIGENCE FOR FUTURES TRADERS</p>
            <h1>Your P&amp;L tells you what happened.<br /><strong>TradePilot Coach tells you why.</strong></h1>
          </section>
          <aside class="panel">
            <p class="panel-kicker"><span class="signal"></span> Execution insight</p>
            <h2>Execution leak detected</h2>
            <p class="summary">Overtrading followed two consecutive losses.</p>
            <div class="metrics">
              <div class="metric"><span class="metric-label">Unnecessary loss</span><span class="metric-value loss">-$1,257</span></div>
              <div class="metric"><span class="metric-label">Discipline</span><span class="metric-value discipline">76%</span></div>
            </div>
            <div class="focus"><span class="focus-label">Next focus</span><p>Wait for confirmation before entry.</p></div>
          </aside>
        </main>
      </body>
    </html>`,
    { waitUntil: "load" }
  );

  await page.locator(".preview").screenshot({ path: outputPath, type: "png" });
} finally {
  await browser.close();
}

console.log(`Generated ${path.relative(projectRoot, outputPath)} (1200x630).`);
