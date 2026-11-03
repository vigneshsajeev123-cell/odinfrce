import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';
import { GoogleGenAI } from '@google/genai';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json({ limit: '10mb' }));

// Initial default mock memories and clusters data for external connectors and fallback context
let appMemories = [
  {
    id: 1,
    type: 'PHOTO',
    title: 'Milky Way Observatory Expedition',
    ocrText: 'ALTITUDE: 3,420M - PARANAL OBSERVATORY - TELESCOPE ARRAY ACTIVE',
    keywords: ['astronomy', 'stars', 'night', 'galaxy', 'observatory'],
    date: '2024-08-14',
    timestamp: 1723651200000,
    gps: { lat: -24.6272, lon: -70.4042 },
    clusterId: 1,
    clusterName: 'Celestial & Astronomy',
    favorite: true
  },
  {
    id: 2,
    type: 'NOTE',
    title: 'Telescope Exposure Calibration Notes',
    content: 'Used 30s exposure at ISO 3200 on the 50mm f/1.8 lens. Zero light pollution in the Atacama desert.',
    keywords: ['camera', 'iso', 'exposure', 'astronomy', 'atacama'],
    date: '2024-08-14',
    timestamp: 1723652800000,
    clusterId: 1,
    clusterName: 'Celestial & Astronomy',
    favorite: false
  },
  {
    id: 3,
    type: 'PHOTO',
    title: 'Alpine Ridge Trail Summit',
    ocrText: 'TRAIL MARKER 42B - ELEVATION 2,890M - MT. FITZ ROY OUTLOOK',
    keywords: ['hiking', 'mountains', 'snow', 'patagonia', 'nature'],
    date: '2024-09-02',
    timestamp: 1725283200000,
    gps: { lat: -49.2714, lon: -73.0431 },
    clusterId: 2,
    clusterName: 'Patagonian Mountain Treks',
    favorite: true
  },
  {
    id: 4,
    type: 'PHOTO_ATTACHED_NOTE',
    title: 'Mountain Pass Weather Change',
    content: 'Glacial winds picked up around 3 PM. Had to tether the bivvy gear before sunset.',
    keywords: ['hiking', 'wind', 'glacier', 'bivvy', 'weather'],
    date: '2024-09-02',
    timestamp: 1725290400000,
    clusterId: 2,
    clusterName: 'Patagonian Mountain Treks',
    favorite: false
  },
  {
    id: 5,
    type: 'PHOTO',
    title: 'Kyoto Bamboo Forest Stroll',
    ocrText: 'ARASHIYAMA BAMBOO GROVE - ENTRANCE PATH',
    keywords: ['japan', 'kyoto', 'bamboo', 'travel', 'zen'],
    date: '2024-10-18',
    timestamp: 1729245600000,
    gps: { lat: 35.0170, lon: 135.6713 },
    clusterId: 3,
    clusterName: 'Kyoto Serenity & Gardens',
    favorite: true
  },
  {
    id: 6,
    type: 'NOTE',
    title: 'Matcha Tea Ceremony Ritual',
    content: 'Learned the 4 principles of Chado: Wa (Harmony), Kei (Respect), Sei (Purity), Jaku (Tranquility).',
    keywords: ['tea', 'matcha', 'kyoto', 'zen', 'culture'],
    date: '2024-10-19',
    timestamp: 1729332000000,
    clusterId: 3,
    clusterName: 'Kyoto Serenity & Gardens',
    favorite: false
  }
];

let appClusters = [
  { id: 1, name: 'Celestial & Astronomy', memoryCount: 2, dominantKeywords: ['astronomy', 'stars', 'observatory'] },
  { id: 2, name: 'Patagonian Mountain Treks', memoryCount: 2, dominantKeywords: ['hiking', 'mountains', 'patagonia'] },
  { id: 3, name: 'Kyoto Serenity & Gardens', memoryCount: 2, dominantKeywords: ['kyoto', 'bamboo', 'tea', 'zen'] }
];

// Helper to get Gemini client
function getGeminiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    console.warn('GEMINI_API_KEY is not set in environment.');
  }
  return new GoogleGenAI({ apiKey: apiKey || '' });
}

// -------------------------------------------------------------
// CONNECTOR API ENDPOINTS FOR EXTERNAL BOTS (ChatGPT, Claude, etc.)
// -------------------------------------------------------------

// Sync data from client state
app.post('/api/connectors/sync', (req, res) => {
  try {
    if (req.body.memories && Array.isArray(req.body.memories)) {
      appMemories = req.body.memories;
    }
    if (req.body.clusters && Array.isArray(req.body.clusters)) {
      appClusters = req.body.clusters;
    }
    res.json({ success: true, memoryCount: appMemories.length, clusterCount: appClusters.length });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Endpoint for ChatGPT / Claude connectors to retrieve memories
app.get('/api/connectors/memories', (req, res) => {
  const { query, clusterId, type } = req.query;
  let results = [...appMemories];

  if (clusterId) {
    results = results.filter(m => m.clusterId === Number(clusterId));
  }
  if (type) {
    results = results.filter(m => m.type === type);
  }
  if (query && typeof query === 'string') {
    const q = query.toLowerCase();
    results = results.filter(m => 
      m.title.toLowerCase().includes(q) ||
      (m.content && m.content.toLowerCase().includes(q)) ||
      (m.ocrText && m.ocrText.toLowerCase().includes(q)) ||
      m.keywords.some(k => k.toLowerCase().includes(q))
    );
  }

  res.json({
    app: 'Memory Universe',
    total: results.length,
    memories: results
  });
});

// Endpoint for ChatGPT / Claude connectors to retrieve cluster galaxies
app.get('/api/connectors/clusters', (req, res) => {
  res.json({
    app: 'Memory Universe',
    total: appClusters.length,
    clusters: appClusters
  });
});

// OpenAPI 3.1 Specification for ChatGPT Custom GPT Actions
app.get('/api/connectors/openapi.json', (req, res) => {
  const hostUrl = process.env.APP_URL || `http://localhost:${PORT}`;
  const openApiSpec = {
    openapi: '3.1.0',
    info: {
      title: 'Memory Universe AI Connector API',
      description: 'Allows ChatGPT, Claude, and external AI agents to query floating 3D memories, photos, notes, OCR texts, and galactic clusters from the user\'s local Memory Universe.',
      version: '1.0.0'
    },
    servers: [
      {
        url: hostUrl,
        description: 'Memory Universe Connector Endpoint'
      }
    ],
    paths: {
      '/api/connectors/memories': {
        get: {
          summary: 'Retrieve memories matching search query or cluster',
          operationId: 'getMemories',
          parameters: [
            {
              name: 'query',
              in: 'query',
              description: 'Semantic keyword or search string to filter memories',
              required: false,
              schema: { type: 'string' }
            },
            {
              name: 'clusterId',
              in: 'query',
              description: 'Filter memories belonging to a specific cluster ID',
              required: false,
              schema: { type: 'integer' }
            },
            {
              name: 'type',
              in: 'query',
              description: 'Memory type: PHOTO, NOTE, or PHOTO_ATTACHED_NOTE',
              required: false,
              schema: { type: 'string', enum: ['PHOTO', 'NOTE', 'PHOTO_ATTACHED_NOTE'] }
            }
          ],
          responses: {
            '200': {
              description: 'List of matching memories with OCR text, GPS coordinates, notes, and tags'
            }
          }
        }
      },
      '/api/connectors/clusters': {
        get: {
          summary: 'Retrieve all galactic clusters/communities discovered by the Leiden algorithm',
          operationId: 'getClusters',
          responses: {
            '200': {
              description: 'List of cluster groups with member counts and dominant keywords'
            }
          }
        }
      }
    }
  };
  res.json(openApiSpec);
});

// Model Context Protocol (MCP) tool manifest for Claude Projects & Claude Desktop
app.get('/api/connectors/mcp.json', (req, res) => {
  const hostUrl = process.env.APP_URL || `http://localhost:${PORT}`;
  const mcpConfig = {
    name: 'memory-universe-connector',
    description: 'Claude Model Context Protocol (MCP) server providing access to Memory Universe offline 3D photo & note memories.',
    version: '1.0.0',
    serverUrl: `${hostUrl}/api/connectors`,
    tools: [
      {
        name: 'search_memories',
        description: 'Searches through memories, photos, OCR extracted texts, and personal notes stored in Memory Universe.',
        parameters: {
          type: 'object',
          properties: {
            query: { type: 'string', description: 'Keyword or concept to search for' },
            type: { type: 'string', enum: ['PHOTO', 'NOTE', 'PHOTO_ATTACHED_NOTE'], description: 'Filter by memory object type' }
          }
        }
      },
      {
        name: 'list_clusters',
        description: 'Lists all memory galaxies/clusters generated by the AI clustering algorithm.',
        parameters: {
          type: 'object',
          properties: {}
        }
      }
    ]
  };
  res.json(mcpConfig);
});

// -------------------------------------------------------------
// CHAT API ENDPOINT USING GEMINI (gemini-3.8-flash)
// -------------------------------------------------------------

app.post('/api/chat', async (req, res) => {
  try {
    const { messages, contextMemories, contextClusters, enableSearch } = req.body;

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: 'Messages array is required' });
    }

    const ai = getGeminiClient();

    // Prepare memory context
    const memoriesToUse = (contextMemories && Array.isArray(contextMemories)) ? contextMemories : appMemories;
    const clustersToUse = (contextClusters && Array.isArray(contextClusters)) ? contextClusters : appClusters;

    const formattedContext = `
YOU ARE THE "MEMORY UNIVERSE AI ORACLE" & PERSONAL MEMORY COPILOT.
You have direct, privileged access to the user's personal memories, photographs, notes, and 3D galaxy clusters from their local Memory Universe application.

CURRENT USER MEMORY CONTEXT:
${memoriesToUse.map((m: any, idx: number) => {
  return `[Memory #${m.id}] Type: ${m.type} | Title: "${m.title}"
  Date: ${m.date || 'Unknown'} | Cluster: "${m.clusterName || ('Galaxy #' + m.clusterId)}"
  ${m.content ? `Note Content: "${m.content}"` : ''}
  ${m.ocrText ? `OCR Extracted Text from Image: "${m.ocrText}"` : ''}
  ${m.gps ? `GPS Coordinates: (${m.gps.lat}, ${m.gps.lon})` : ''}
  Keywords: [${(m.keywords || []).join(', ')}]
  Favorite: ${m.favorite ? 'Yes' : 'No'}`;
}).join('\n\n')}

GALACTIC CLUSTERS (LEIDEN COMMUNITY DETECTION):
${clustersToUse.map((c: any) => `- Galaxy "${c.name}" (ID ${c.id}): ${c.memoryCount || 0} memories, Key themes: ${(c.dominantKeywords || []).join(', ')}`).join('\n')}

GUIDELINES:
1. Answer the user's questions intimately, accurately, and thoughtfully based on their photos, OCR text, notes, GPS locations, and dates.
2. If the user asks about places, trails, camera settings, or notes, cite the exact memory title and memory ID.
3. If the user asks questions requiring external knowledge or current real-world facts (e.g. historical facts about Paranal Observatory, current weather in Patagonia, or Kyoto travel tips), use your Google Search grounding when available to augment your answer.
4. Keep answers friendly, evocative, well-structured, and helpful.
`;

    // Map conversation history to Gemini contents format
    // Last message is the current user prompt; prior messages are conversation turns.
    const contents = messages.map((msg: { role: string; content: string }) => ({
      role: msg.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: msg.content }]
    }));

    // Call Gemini 3.8 Flash model per gemini-api skill instructions
    const config: any = {
      systemInstruction: formattedContext,
    };

    if (enableSearch) {
      config.tools = [{ googleSearch: {} }];
    }

    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents: contents,
      config: config
    });

    const replyText = response.text || 'I explored your memories, but could not formulate a response.';
    
    // Extract search sources if grounding metadata exists
    let sources: Array<{ title: string; uri: string }> = [];
    const groundingMetadata = response.candidates?.[0]?.groundingMetadata;
    if (groundingMetadata?.groundingChunks) {
      sources = groundingMetadata.groundingChunks
        .map((chunk: any) => chunk.web)
        .filter((web: any) => web && web.uri)
        .map((web: any) => ({ title: web.title || web.uri, uri: web.uri }));
    }

    res.json({
      reply: replyText,
      sources: sources,
      modelUsed: 'gemini-3.8-flash'
    });
  } catch (error: any) {
    console.error('Error generating chat response:', error);
    res.status(500).json({ 
      error: error.message || 'Failed to communicate with AI',
      details: error.toString()
    });
  }
});

// Setup Vite dev middleware or static serving
async function setupServer() {
  if (process.env.NODE_ENV === 'production') {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.resolve(__dirname, 'dist', 'index.html'));
    });
  } else {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: {
        middlewareMode: true,
        hmr: process.env.DISABLE_HMR !== 'true',
        watch: process.env.DISABLE_HMR === 'true' ? null : {},
      },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, () => {
    console.log(`Memory Universe Server listening on http://0.0.0.0:${PORT}`);
  });
}

setupServer();
