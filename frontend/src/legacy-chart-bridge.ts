import {
  createChart,
  LineStyle,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts';

type Candle = {
  date: string;
  timestamp: string;
  open: number;
  high: number;
  low: number;
  close: number;
};
type Levels = { S1: number | string; S2: number | string; R1: number | string; R2: number | string };
type ChartState = { date: string; candles: Candle[]; levels: Levels | null };
type LegacyWindow = Window & { __niftyLabGetSpotChartState?: () => ChartState; spotDrawChart?: () => void };

const frameWindow = window as LegacyWindow;
const canvas = document.getElementById('niftySpotLevelsCanvas') as HTMLCanvasElement | null;
if (canvas) {
  const chartHost = document.createElement('div');
  chartHost.id = 'niftySpotLightweightChart';
  chartHost.setAttribute('aria-label', 'NIFTY 50 five-minute candlestick chart with support and resistance overlays');
  chartHost.style.cssText = 'display:none;width:100%;height:100%;min-height:300px;';
  canvas.parentElement?.insertBefore(chartHost, canvas);

  let chart: IChartApi | null = null;
  let candlesSeries: ISeriesApi<'Candlestick'> | null = null;
  const levelSeries = new Map<string, ISeriesApi<'Line'>>();
  let resizeObserver: ResizeObserver | null = null;
  let lastViewKey = '';

  const toEpoch = (raw: string): UTCTimestamp | null => {
    const input = String(raw || '').trim();
    // Lightweight Charts needs a real intraday timestamp. Date-only rows stay
    // on the original fallback chart rather than receiving fabricated times.
    if (!/\d{1,2}:\d{2}/.test(input)) return null;
    const normalized = input.includes('T') ? input : input.replace(' ', 'T');
    const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const parsed = Date.parse(hasZone ? normalized : `${normalized}+05:30`);
    if (!Number.isFinite(parsed)) return null;
    return Math.floor(parsed / 1000) as UTCTimestamp;
  };

  const cssValue = (name: string, fallback: string) => {
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return value || fallback;
  };

  const showFallback = () => {
    chartHost.style.display = 'none';
    canvas.style.display = 'block';
  };

  const ensureChart = () => {
    if (chart) return;
    const rect = chartHost.getBoundingClientRect();
    if (rect.width < 60 || rect.height < 100) return;
    chart = createChart(chartHost, {
      width: Math.floor(rect.width),
      height: Math.floor(rect.height),
      layout: {
        background: { color: cssValue('--surface', '#ffffff') },
        textColor: cssValue('--chart-label', '#697995'),
        fontFamily: 'Inter, ui-sans-serif, system-ui, sans-serif',
        fontSize: 11,
      },
      grid: {
        vertLines: { color: cssValue('--chart-grid', '#e1e7f0') },
        horzLines: { color: cssValue('--chart-grid', '#e1e7f0') },
      },
      rightPriceScale: { borderColor: cssValue('--app-line', '#e1e7f0'), scaleMargins: { top: 0.08, bottom: 0.1 } },
      timeScale: { borderColor: cssValue('--app-line', '#e1e7f0'), timeVisible: true, secondsVisible: false, rightOffset: 2 },
      crosshair: { vertLine: { color: cssValue('--chart-line', '#3c66f5'), labelBackgroundColor: cssValue('--app-accent', '#3c66f5') }, horzLine: { color: cssValue('--chart-line', '#3c66f5'), labelBackgroundColor: cssValue('--app-accent', '#3c66f5') } },
      localization: { priceFormatter: (price: number) => price.toLocaleString('en-IN', { maximumFractionDigits: 2 }) },
    });
    candlesSeries = chart.addCandlestickSeries({
      upColor: '#16805f', downColor: '#b93459',
      borderUpColor: '#16805f', borderDownColor: '#b93459',
      wickUpColor: '#16805f', wickDownColor: '#b93459',
      lastValueVisible: true, priceLineVisible: false,
    });
    const parent = canvas.parentElement;
    if (parent && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        if (!chart) return;
        const size = chartHost.getBoundingClientRect();
        if (size.width > 0 && size.height > 0) chart.applyOptions({ width: Math.floor(size.width), height: Math.floor(size.height) });
      });
      resizeObserver.observe(parent);
    }
  };

  const render = () => {
    const state = frameWindow.__niftyLabGetSpotChartState?.();
    if (!state || !state.date || !Array.isArray(state.candles) || state.candles.length === 0 || !state.levels) {
      showFallback();
      return;
    }
    const rows = [...state.candles].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
    const candleData: Array<{ time: UTCTimestamp; open: number; high: number; low: number; close: number }> = [];
    let previousTime = -Infinity;
    for (const row of rows) {
      const time = toEpoch(row.timestamp);
      const values = [row.open, row.high, row.low, row.close].map(Number);
      if (!time || values.some((value) => !Number.isFinite(value)) || values[2] > values[1] || values[2] > Math.min(values[0], values[3]) || values[1] < Math.max(values[0], values[3]) || Number(time) <= previousTime) {
        // Never silently rewrite malformed/duplicate chart records. Let the original renderer
        // display the source rows through its existing fallback behaviour instead.
        showFallback();
        return;
      }
      previousTime = Number(time);
      candleData.push({ time, open: values[0], high: values[1], low: values[2], close: values[3] });
    }
    const levelNames = ['S1', 'S2', 'R1', 'R2'] as const;
    const numericLevels = Object.fromEntries(levelNames.map((name) => [name, Number(state.levels?.[name])])) as Record<typeof levelNames[number], number>;
    if (levelNames.some((name) => !Number.isFinite(numericLevels[name]))) {
      showFallback();
      return;
    }

    chartHost.style.display = 'block';
    canvas.style.display = 'none';
    ensureChart();
    if (!chart || !candlesSeries) {
      showFallback();
      return;
    }
    const currentSize = chartHost.getBoundingClientRect();
    if (currentSize.width > 0 && currentSize.height > 0) {
      chart.applyOptions({ width: Math.floor(currentSize.width), height: Math.floor(currentSize.height) });
    }
    chart.applyOptions({
      layout: { background: { color: cssValue('--surface', '#ffffff') }, textColor: cssValue('--chart-label', '#697995') },
      grid: { vertLines: { color: cssValue('--chart-grid', '#e1e7f0') }, horzLines: { color: cssValue('--chart-grid', '#e1e7f0') } },
      rightPriceScale: { borderColor: cssValue('--app-line', '#e1e7f0') },
      timeScale: { borderColor: cssValue('--app-line', '#e1e7f0') },
    });
    candlesSeries.setData(candleData);
    const firstTime = candleData[0].time;
    const lastTime = (Number(candleData[candleData.length - 1].time) + 300) as UTCTimestamp;
    const palette: Record<string, { color: string; style: LineStyle }> = {
      S1: { color: '#d43b57', style: LineStyle.Solid }, S2: { color: '#f08b45', style: LineStyle.Dashed },
      R1: { color: '#07875b', style: LineStyle.Solid }, R2: { color: '#087d87', style: LineStyle.Dashed },
    };
    for (const name of levelNames) {
      let line = levelSeries.get(name);
      if (!line) {
        line = chart.addLineSeries({
          title: name,
          color: palette[name].color,
          lineWidth: 2,
          lineStyle: palette[name].style,
          lastValueVisible: true,
          priceLineVisible: false,
          crosshairMarkerVisible: false,
        });
        levelSeries.set(name, line);
      }
      line.setData([{ time: firstTime, value: numericLevels[name] }, { time: lastTime, value: numericLevels[name] }]);
    }
    const viewKey = `${state.date}|${candleData.length}|${candleData[0].time}|${candleData[candleData.length - 1].time}`;
    if (viewKey !== lastViewKey) {
      chart.timeScale().fitContent();
      lastViewKey = viewKey;
    }
  };

  // Keep the original chart function in place; it remains the fallback and is
  // also the signal that the user's date, upload, or level data has changed.
  const originalDraw = frameWindow.spotDrawChart;
  if (typeof originalDraw === 'function') {
    frameWindow.spotDrawChart = () => {
      originalDraw();
      render();
    };
  }
  window.addEventListener('resize', render);
  window.addEventListener('storage', render);
  render();
}
