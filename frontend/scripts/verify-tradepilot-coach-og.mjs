import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..");
const socialImage = "https://www.tradepilotcoach.com/assets/tradepilot-coach-og.png";
const imagePath = path.join(projectRoot, "public", "assets", "tradepilot-coach-og.png");
const homepagePath = path.join(projectRoot, "dist", "index.html");

const image = await readFile(imagePath);
const pngSignature = "89504e470d0a1a0a";
if (image.subarray(0, 8).toString("hex") !== pngSignature) {
  throw new Error("TradePilot Coach social preview is not a PNG.");
}

const width = image.readUInt32BE(16);
const height = image.readUInt32BE(20);
if (width !== 1200 || height !== 630) {
  throw new Error(`TradePilot Coach social preview must be 1200x630; received ${width}x${height}.`);
}

const homepage = await readFile(homepagePath, "utf8");
if (!homepage.includes(socialImage)) {
  throw new Error("Prerendered homepage is missing the TradePilot Coach social preview URL.");
}
if (homepage.includes("tradepilot-logo-homepage.png")) {
  throw new Error("Prerendered homepage still contains the legacy social image URL.");
}

console.log("Verified TradePilot Coach social preview asset and homepage prerender metadata.");
