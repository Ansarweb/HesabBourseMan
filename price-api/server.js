const express = require("express");

const app = express();
const PORT = process.env.PORT || 3000;

const SEARCH_URL =
  "https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/";

const PRICE_URL =
  "https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/";

const cache = new Map();
const CACHE_TIME = 60 * 1000;

async function requestJson(url) {
  const response = await fetch(url, {
    headers: {
      "User-Agent": "Mozilla/5.0",
      "Accept": "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(`TSETMC HTTP ${response.status}`);
  }

  return await response.json();
}

function findInsCode(data, wantedSymbol) {
  const arrays = [
    data.instrumentSearch,
    data.instrumentSearchResults,
    data.instrument
  ];

  for (const array of arrays) {
    if (!Array.isArray(array)) continue;

    for (const item of array) {
      const symbol =
        item.lVal18AFC ||
        item.symbol ||
        "";

      const name =
        item.lVal30 ||
        item.name ||
        "";

      if (
        String(symbol).trim().toUpperCase() ===
          wantedSymbol.trim().toUpperCase() ||
        String(name).trim().toUpperCase() ===
          wantedSymbol.trim().toUpperCase()
      ) {
        return (
          item.insCode ||
          item.instrumentID ||
          ""
        );
      }
    }
  }

  return "";
}

async function getPrice(symbol) {
  const cleanSymbol = String(symbol || "").trim();

  if (!cleanSymbol) {
    throw new Error("symbol is required");
  }

  const cached = cache.get(cleanSymbol);

  if (cached && Date.now() - cached.time < CACHE_TIME) {
    return cached.data;
  }

  const searchUrl =
    SEARCH_URL + encodeURIComponent(cleanSymbol);

  const searchData = await requestJson(searchUrl);

  const insCode =
    findInsCode(searchData, cleanSymbol);

  if (!insCode) {
    throw new Error(
      `Instrument not found: ${cleanSymbol}`
    );
  }

  const priceData =
    await requestJson(PRICE_URL + insCode);

  const info =
    priceData.closingPriceInfo || {};

  let lastPrice =
    Number(info.pDrCotVal || 0);

  const closingPrice =
    Number(info.pClosing || 0);

  if (lastPrice <= 0) {
    lastPrice = closingPrice;
  }

  if (lastPrice <= 0) {
    throw new Error(
      `No valid price for ${cleanSymbol}`
    );
  }

  const result = {
    symbol: cleanSymbol,
    insCode: insCode,
    lastPrice: lastPrice,
    closingPrice: closingPrice,
    updatedAt: new Date().toISOString()
  };

  cache.set(cleanSymbol, {
    time: Date.now(),
    data: result
  });

  return result;
}

app.get("/", (req, res) => {
  res.json({
    ok: true,
    service: "Hesab Bourse TSETMC Price API"
  });
});

app.get("/price", async (req, res) => {
  try {
    const symbol = req.query.symbol;

    if (!symbol) {
      return res.status(400).json({
        ok: false,
        error: "symbol is required"
      });
    }

    const result = await getPrice(symbol);

    res.json({
      ok: true,
      ...result
    });
  } catch (error) {
    res.status(500).json({
      ok: false,
      error: error.message || "Unknown error"
    });
  }
});

app.get("/prices", async (req, res) => {
  try {
    const symbolsText = String(
      req.query.symbols || ""
    );

    const symbols = symbolsText
      .split(",")
      .map(x => x.trim())
      .filter(Boolean);

    if (symbols.length === 0) {
      return res.status(400).json({
        ok: false,
        error: "symbols is required"
      });
    }

    const results = [];

    for (const symbol of symbols) {
      try {
        const price = await getPrice(symbol);
        results.push(price);
      } catch (error) {
        results.push({
          symbol,
          error: error.message || "Price unavailable"
        });
      }
    }

    res.json({
      ok: true,
      count: results.length,
      prices: results
    });
  } catch (error) {
    res.status(500).json({
      ok: false,
      error: error.message || "Unknown error"
    });
  }
});

app.listen(PORT, () => {
  console.log(
    `Price API running on port ${PORT}`
  );
});
