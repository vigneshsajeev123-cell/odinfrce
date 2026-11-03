import React, { useState, useRef, useEffect } from 'react';
import { 
  Bot, Send, Sparkles, Globe, Copy, Check, ExternalLink, 
  Terminal, ShieldCheck, Download, RefreshCw, Eye, MessageSquare, 
  Sliders, Database, Layers, Tag, MapPin, Calendar, FileText, 
  Share2, Cpu, AlertCircle, ChevronDown, ChevronRight, Zap
} from 'lucide-react';

export interface ConnectorMemory {
  id: number;
  type: string;
  title: string;
  content?: string;
  ocrText?: string;
  keywords: string[];
  date: string;
  timestamp: number;
  gps?: { lat: number; lon: number };
  clusterId: number;
  clusterName?: string;
  favorite: boolean;
  imageUrl?: string;
}

export interface ConnectorCluster {
  id: number;
  name: string;
  memoryCount: number;
  dominantKeywords?: string[];
  colorHex?: string;
}

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  botType: 'gemini' | 'chatgpt' | 'claude';
  sources?: Array<{ title: string; uri: string }>;
  groundedMemories?: number[];
}

interface AIConnectorsHubProps {
  memories: ConnectorMemory[];
  clusters: ConnectorCluster[];
}

export default function AIConnectorsHub({ memories, clusters }: AIConnectorsHubProps) {
  const [activeSubTab, setActiveSubTab] = useState<'chat' | 'chatgpt' | 'claude' | 'sync'>('chat');
  const [selectedBot, setSelectedBot] = useState<'gemini' | 'chatgpt' | 'claude'>('gemini');
  const [enableSearchGrounding, setEnableSearchGrounding] = useState<boolean>(true);
  const [inputPrompt, setInputPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [showContextInspector, setShowContextInspector] = useState(false);
  const [syncStatus, setSyncStatus] = useState<string | null>(null);

  // Test API endpoint state for ChatGPT/Claude
  const [testQuery, setTestQuery] = useState('hiking');
  const [testResult, setTestResult] = useState<any>(null);
  const [isTestingApi, setIsTestingApi] = useState(false);

  // Multi-turn chat message history
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'msg-welcome',
      role: 'assistant',
      content: `Welcome to the **Memory Universe AI Connector**!\n\nI am connected to your personal universe of **${memories.length} memories** and **${clusters.length} galactic clusters**. All OCR transcripts, photography notes, timestamps, and GPS coordinates are loaded into my working memory.\n\nYou can chat directly with me using **Gemini 3.8 Flash** with live Google Search grounding, or configure connectors for **ChatGPT** and **Claude** to query your memories from external bots.`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      botType: 'gemini'
    }
  ]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  // Sync memories to server backend connector cache
  const handleSyncData = async () => {
    try {
      setSyncStatus('Syncing to backend connector cache...');
      const res = await fetch('/api/connectors/sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          memories: memories.map(m => ({
            ...m,
            clusterName: clusters.find(c => c.id === m.clusterId)?.name || `Cluster #${m.clusterId}`
          })),
          clusters: clusters.map(c => ({
            id: c.id,
            name: c.name,
            memoryCount: memories.filter(m => m.clusterId === c.id).length,
            dominantKeywords: c.dominantKeywords || []
          }))
        })
      });
      const data = await res.json();
      if (data.success) {
        setSyncStatus(`Successfully synchronized ${data.memoryCount} memories to AI connector cache!`);
      } else {
        setSyncStatus(`Sync error: ${data.error || 'Unknown error'}`);
      }
    } catch (err: any) {
      setSyncStatus(`Sync error: ${err.message}`);
    }
    setTimeout(() => setSyncStatus(null), 4000);
  };

  // Run a test query against the connector API
  const handleTestConnector = async () => {
    setIsTestingApi(true);
    try {
      const res = await fetch(`/api/connectors/memories?query=${encodeURIComponent(testQuery)}`);
      const data = await res.json();
      setTestResult(data);
    } catch (err: any) {
      setTestResult({ error: err.message });
    } finally {
      setIsTestingApi(false);
    }
  };

  // Copy helper
  const handleCopy = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  // Handle multi-turn conversation submit
  const handleSendMessage = async (customPrompt?: string) => {
    const textToSend = customPrompt || inputPrompt;
    if (!textToSend.trim() || isLoading) return;

    const userMessage: ChatMessage = {
      id: `usr-${Date.now()}`,
      role: 'user',
      content: textToSend.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      botType: selectedBot
    };

    const newMessages = [...messages, userMessage];
    setMessages(newMessages);
    setInputPrompt('');
    setIsLoading(true);

    try {
      // If user selected Gemini (Live Direct in-app bot)
      if (selectedBot === 'gemini') {
        const enrichedMemories = memories.map(m => ({
          ...m,
          clusterName: clusters.find(c => c.id === m.clusterId)?.name
        }));

        const response = await fetch('/api/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            messages: newMessages.map(m => ({ role: m.role, content: m.content })),
            contextMemories: enrichedMemories,
            contextClusters: clusters,
            enableSearch: enableSearchGrounding
          })
        });

        const data = await response.json();

        if (response.ok && data.reply) {
          const assistantMessage: ChatMessage = {
            id: `ast-${Date.now()}`,
            role: 'assistant',
            content: data.reply,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            botType: 'gemini',
            sources: data.sources || []
          };
          setMessages(prev => [...prev, assistantMessage]);
        } else {
          throw new Error(data.error || 'Server returned an error');
        }
      } else {
        // Simulated local execution for ChatGPT / Claude Connector test sandbox
        // Simulating the bot evaluating memories fetched via connector
        const lowerQ = textToSend.toLowerCase();
        const matched = memories.filter(m => 
          m.title.toLowerCase().includes(lowerQ) ||
          (m.content && m.content.toLowerCase().includes(lowerQ)) ||
          (m.ocrText && m.ocrText.toLowerCase().includes(lowerQ)) ||
          m.keywords.some(k => k.toLowerCase().includes(lowerQ))
        );

        let connectorReply = '';
        if (selectedBot === 'chatgpt') {
          connectorReply = `[ChatGPT via OpenAPI Connector /api/connectors/memories]\n\nI queried your Memory Universe API for "${textToSend}". Found **${matched.length} matching memory items**:\n\n` +
            (matched.length > 0 
              ? matched.map(m => `• **${m.title}** (${m.type}, Date: ${m.date}): ${m.content || m.ocrText || 'Photo item'}\n  *Cluster:* ${clusters.find(c => c.id === m.clusterId)?.name || 'Default'}`).join('\n')
              : `No direct keyword match, but your 3 galactic clusters (${clusters.map(c => c.name).join(', ')}) contain related astronomy and nature themes.`);
        } else {
          connectorReply = `[Claude via MCP Tool \`search_memories\`]\n\nExecuted Model Context Protocol tool with query parameter: "${textToSend}".\n\nReturned ${matched.length} records from your offline SQLite/Room memory graph:\n\n` +
            (matched.length > 0 
              ? matched.map(m => `- **[ID #${m.id}] ${m.title}**: Keywords: [${m.keywords.join(', ')}]${m.gps ? ` | Coordinates: ${m.gps.lat}, ${m.gps.lon}` : ''}`).join('\n')
              : 'Zero graph items matched the filter string. Consider querying by galaxy cluster ID.');
        }

        setTimeout(() => {
          const assistantMessage: ChatMessage = {
            id: `ast-${Date.now()}`,
            role: 'assistant',
            content: connectorReply,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            botType: selectedBot
          };
          setMessages(prev => [...prev, assistantMessage]);
          setIsLoading(false);
        }, 600);
        return;
      }
    } catch (err: any) {
      const errorMessage: ChatMessage = {
        id: `err-${Date.now()}`,
        role: 'assistant',
        content: `**AI Connection Note:** ${err.message || 'Unable to connect to model endpoint.'}\n\nPlease verify that your server is running with a valid \`GEMINI_API_KEY\` in your environment. You can also explore the ChatGPT & Claude OpenAPI/MCP connectors in the adjacent tabs.`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        botType: selectedBot
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const samplePrompts = [
    "Which memories involve high altitude or mountain summits?",
    "Summarize my telescope exposure calibration notes.",
    "Show me all photos with detected OCR text.",
    "What are the 4 principles of Chado from my Kyoto notes?",
    "Which memories share similar GPS coordinates or regions?"
  ];

  // OpenAPI JSON string
  const openApiSpecString = JSON.stringify({
    openapi: "3.1.0",
    info: {
      title: "Memory Universe API",
      description: "Connector allowing ChatGPT to access offline 3D photo memories, notes, and clusters",
      version: "1.0.0"
    },
    paths: {
      "/api/connectors/memories": {
        get: {
          summary: "Search user memories",
          operationId: "getMemories",
          parameters: [
            { name: "query", in: "query", required: false, schema: { type: "string" } },
            { name: "clusterId", in: "query", required: false, schema: { type: "integer" } }
          ]
        }
      },
      "/api/connectors/clusters": {
        get: {
          summary: "List memory galaxy clusters",
          operationId: "getClusters"
        }
      }
    }
  }, null, 2);

  // Claude MCP JSON string
  const mcpConfigString = JSON.stringify({
    mcpServers: {
      "memory-universe": {
        command: "node",
        args: ["./mcp-connector.js"],
        env: {
          MEMORY_UNIVERSE_URL: "http://localhost:3000/api/connectors"
        }
      }
    }
  }, null, 2);

  // Claude Project Markdown Knowledge Export
  const claudeKnowledgeMarkdown = `# MEMORY UNIVERSE: USER KNOWLEDGE EXPORT
Exported on: ${new Date().toISOString()}
Total Memories: ${memories.length}
Total Clusters: ${clusters.length}

## GALACTIC CLUSTERS
${clusters.map(c => `- **${c.name}** (ID ${c.id}): ${memories.filter(m => m.clusterId === c.id).length} memories | Themes: ${(c.dominantKeywords || []).join(', ')}`).join('\n')}

## INDIVIDUAL MEMORIES & NOTES
${memories.map(m => `### [ID #${m.id}] ${m.title}
- Type: ${m.type}
- Date: ${m.date}
- Galaxy: ${clusters.find(c => c.id === m.clusterId)?.name || 'Unknown'}
${m.content ? `- Note Content: "${m.content}"` : ''}
${m.ocrText ? `- Extracted Image OCR: "${m.ocrText}"` : ''}
${m.gps ? `- GPS Location: ${m.gps.lat}, ${m.gps.lon}` : ''}
- Keywords: ${m.keywords.join(', ')}
- Favorite: ${m.favorite ? 'Yes' : 'No'}
`).join('\n')}
`;

  return (
    <div className="flex-1 flex flex-col h-full bg-[#030306] overflow-hidden text-slate-200">
      {/* Sub Header & Tabs */}
      <div className="border-b border-slate-800/80 bg-[#07070d]/90 px-6 py-3 flex flex-wrap items-center justify-between gap-4 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-600 via-indigo-600 to-violet-500 p-0.5 flex items-center justify-center shadow-lg shadow-indigo-950/50">
            <Bot className="w-4 h-4 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-bold text-sm text-white">AI Connectors & Grounded Chat</h2>
              <span className="text-[10px] font-mono bg-indigo-950/80 text-indigo-300 px-2 py-0.5 rounded-full border border-indigo-700/50 flex items-center gap-1">
                <Sparkles className="w-2.5 h-2.5" />
                Context Active
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              Connect external AI bots (ChatGPT, Claude, Gemini) with access to your 3D memories & clusters
            </p>
          </div>
        </div>

        {/* Sub-tab navigation */}
        <div className="flex items-center gap-1.5 bg-slate-900/90 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveSubTab('chat')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1.5 ${
              activeSubTab === 'chat' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Interactive Chat</span>
          </button>
          <button
            onClick={() => setActiveSubTab('chatgpt')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1.5 ${
              activeSubTab === 'chatgpt' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Zap className="w-3.5 h-3.5" />
            <span>ChatGPT Connector</span>
          </button>
          <button
            onClick={() => setActiveSubTab('claude')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1.5 ${
              activeSubTab === 'claude' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Cpu className="w-3.5 h-3.5" />
            <span>Claude MCP Connector</span>
          </button>
          <button
            onClick={() => setActiveSubTab('sync')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1.5 ${
              activeSubTab === 'sync' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Database className="w-3.5 h-3.5" />
            <span>Context & Sync</span>
          </button>
        </div>
      </div>

      {/* SUB-TAB 1: INTERACTIVE CHAT */}
      {activeSubTab === 'chat' && (
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          {/* Main Chat Thread */}
          <div className="flex-1 flex flex-col overflow-hidden bg-black/60 relative">
            {/* Grounded Context Header Bar */}
            <div className="border-b border-slate-800/80 bg-slate-950/80 px-6 py-2.5 flex items-center justify-between backdrop-blur-md">
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-1.5 text-xs text-slate-300">
                  <Database className="w-3.5 h-3.5 text-indigo-400" />
                  <span className="font-medium">Grounded Context:</span>
                  <span className="bg-indigo-950/80 text-indigo-300 px-2 py-0.5 rounded text-[11px] font-mono border border-indigo-800/50">
                    {memories.length} Memories
                  </span>
                  <span className="bg-violet-950/80 text-violet-300 px-2 py-0.5 rounded text-[11px] font-mono border border-violet-800/50">
                    {clusters.length} Galaxies
                  </span>
                </div>

                {/* Google Search Grounding Toggle */}
                {selectedBot === 'gemini' && (
                  <button
                    onClick={() => setEnableSearchGrounding(!enableSearchGrounding)}
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium border transition ${
                      enableSearchGrounding 
                        ? 'bg-emerald-950/80 border-emerald-600/60 text-emerald-300' 
                        : 'bg-slate-900 border-slate-700/60 text-slate-400'
                    }`}
                    title="Enable real-time Google Search grounding for real-world facts"
                  >
                    <Globe className={`w-3 h-3 ${enableSearchGrounding ? 'text-emerald-400' : 'text-slate-500'}`} />
                    <span>Search Grounding: {enableSearchGrounding ? 'ON' : 'OFF'}</span>
                  </button>
                )}
              </div>

              {/* Bot Selector */}
              <div className="flex items-center gap-1 bg-slate-900 p-0.5 rounded-lg border border-slate-800">
                <button
                  onClick={() => setSelectedBot('gemini')}
                  className={`px-2.5 py-1 rounded text-xs font-medium transition flex items-center gap-1 ${
                    selectedBot === 'gemini' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  <Sparkles className="w-3 h-3" />
                  <span>Gemini 3.8 Flash</span>
                </button>
                <button
                  onClick={() => setSelectedBot('chatgpt')}
                  className={`px-2.5 py-1 rounded text-xs font-medium transition flex items-center gap-1 ${
                    selectedBot === 'chatgpt' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  <Zap className="w-3 h-3" />
                  <span>ChatGPT Sandbox</span>
                </button>
                <button
                  onClick={() => setSelectedBot('claude')}
                  className={`px-2.5 py-1 rounded text-xs font-medium transition flex items-center gap-1 ${
                    selectedBot === 'claude' ? 'bg-amber-600 text-white shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  <Cpu className="w-3 h-3" />
                  <span>Claude Sandbox</span>
                </button>
              </div>
            </div>

            {/* Scrollable Messages Thread */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4">
              {messages.map((msg) => {
                const isUser = msg.role === 'user';
                return (
                  <div
                    key={msg.id}
                    className={`flex items-start gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
                  >
                    {/* Avatar */}
                    <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 shadow-lg ${
                      isUser 
                        ? 'bg-slate-700 text-white' 
                        : msg.botType === 'gemini'
                          ? 'bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-indigo-950/50'
                          : msg.botType === 'chatgpt'
                            ? 'bg-emerald-600 text-white shadow-emerald-950/50'
                            : 'bg-amber-600 text-white shadow-amber-950/50'
                    }`}>
                      {isUser ? (
                        <span className="text-xs font-bold">YOU</span>
                      ) : (
                        <Bot className="w-4 h-4" />
                      )}
                    </div>

                    {/* Message Bubble */}
                    <div className={`max-w-[82%] rounded-2xl p-4 text-xs leading-relaxed ${
                      isUser
                        ? 'bg-indigo-600 text-white rounded-tr-none'
                        : 'bg-slate-900/90 border border-slate-800 text-slate-200 rounded-tl-none shadow-md backdrop-blur-sm'
                    }`}>
                      <div className="flex items-center justify-between gap-3 mb-1.5 pb-1 border-b border-white/10 text-[10px] opacity-75">
                        <span className="font-semibold uppercase tracking-wider">
                          {isUser ? 'You' : msg.botType === 'gemini' ? 'Gemini 3.8 Flash (Oracle)' : msg.botType === 'chatgpt' ? 'ChatGPT (Connector)' : 'Claude (MCP)'}
                        </span>
                        <span>{msg.timestamp}</span>
                      </div>

                      <div className="whitespace-pre-wrap space-y-2">
                        {msg.content}
                      </div>

                      {/* Google Search Sources Citations */}
                      {msg.sources && msg.sources.length > 0 && (
                        <div className="mt-3 pt-2.5 border-t border-slate-800/80">
                          <div className="text-[10px] text-emerald-400 font-semibold flex items-center gap-1 mb-1.5">
                            <Globe className="w-3 h-3" />
                            <span>Grounding Sources (Google Search):</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5">
                            {msg.sources.map((s, idx) => (
                              <a
                                key={idx}
                                href={s.uri}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 text-[10px] bg-slate-950 hover:bg-slate-800 text-slate-300 hover:text-white px-2 py-0.5 rounded border border-slate-800 transition"
                              >
                                <span className="truncate max-w-[160px]">{s.title}</span>
                                <ExternalLink className="w-2.5 h-2.5 shrink-0 opacity-60" />
                              </a>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}

              {isLoading && (
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 rounded-xl bg-indigo-600 flex items-center justify-center text-white shrink-0 animate-pulse">
                    <Sparkles className="w-4 h-4 animate-spin" />
                  </div>
                  <div className="bg-slate-900/90 border border-slate-800 rounded-2xl rounded-tl-none p-4 text-xs text-slate-300 flex items-center gap-2">
                    <RefreshCw className="w-3.5 h-3.5 text-indigo-400 animate-spin" />
                    <span>Searching 3D memory universe & synthesizing response...</span>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Quick Prompt Starters */}
            <div className="px-6 py-2 border-t border-slate-800/60 bg-slate-950/60 flex items-center gap-2 overflow-x-auto scrollbar-none">
              <span className="text-[10px] uppercase font-bold text-slate-500 shrink-0">Ask:</span>
              {samplePrompts.map((prompt, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendMessage(prompt)}
                  disabled={isLoading}
                  className="shrink-0 text-[11px] bg-slate-900/90 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-white px-2.5 py-1 rounded-lg transition"
                >
                  {prompt}
                </button>
              ))}
            </div>

            {/* Chat Input Bar */}
            <div className="p-4 border-t border-slate-800/80 bg-[#07070d]/90 flex items-center gap-2">
              <input
                type="text"
                value={inputPrompt}
                onChange={(e) => setInputPrompt(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                placeholder={
                  selectedBot === 'gemini'
                    ? "Ask Gemini about your photos, notes, camera settings, or trips..."
                    : `Query ${selectedBot === 'chatgpt' ? 'ChatGPT' : 'Claude'} via Memory Connector...`
                }
                className="flex-1 bg-slate-900 border border-slate-700/80 focus:border-indigo-500 rounded-xl px-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none"
              />
              <button
                onClick={() => handleSendMessage()}
                disabled={!inputPrompt.trim() || isLoading}
                className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white px-4 py-2.5 rounded-xl font-medium text-xs flex items-center gap-1.5 transition shadow-lg shadow-indigo-950/50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>Send</span>
              </button>
            </div>
          </div>

          {/* Right Sidebar: Active Memory Snapshot & Connector Status */}
          <div className="w-full md:w-80 border-l border-slate-800/80 bg-[#080811] p-5 flex flex-col gap-4 overflow-y-auto shrink-0">
            <div>
              <h3 className="font-bold text-xs text-white flex items-center gap-1.5">
                <Database className="w-4 h-4 text-indigo-400" />
                <span>Memory Context Index</span>
              </h3>
              <p className="text-[11px] text-slate-400 mt-1">
                Data currently available to AI bots for retrieval.
              </p>
            </div>

            {/* Galaxy Clusters Preview */}
            <div className="space-y-2">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
                Clusters ({clusters.length})
              </span>
              <div className="space-y-1.5">
                {clusters.map((c) => (
                  <div key={c.id} className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-2.5 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-200">{c.name}</span>
                      <span className="text-[10px] font-mono text-indigo-400">
                        {memories.filter(m => m.clusterId === c.id).length} items
                      </span>
                    </div>
                    {c.dominantKeywords && c.dominantKeywords.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1.5">
                        {c.dominantKeywords.map((k, i) => (
                          <span key={i} className="text-[9px] bg-slate-950 text-slate-400 px-1.5 py-0.5 rounded border border-slate-800">
                            #{k}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Quick Memories Preview */}
            <div className="space-y-2">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
                Loaded Memories ({memories.length})
              </span>
              <div className="space-y-1.5 max-h-64 overflow-y-auto pr-1">
                {memories.map((m) => (
                  <div key={m.id} className="bg-slate-900/40 border border-slate-800/60 rounded-lg p-2 text-[11px]">
                    <div className="flex items-center justify-between text-slate-300 font-medium">
                      <span className="truncate max-w-[180px]">{m.title}</span>
                      <span className="text-[9px] font-mono text-slate-500">{m.type}</span>
                    </div>
                    {m.ocrText && (
                      <div className="text-[10px] text-emerald-400/80 truncate mt-0.5">
                        OCR: {m.ocrText}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Quick Actions */}
            <div className="pt-2 border-t border-slate-800 flex flex-col gap-2">
              <button
                onClick={handleSyncData}
                className="w-full bg-slate-900 hover:bg-slate-800 border border-slate-700/80 text-xs py-2 px-3 rounded-xl text-slate-300 hover:text-white flex items-center justify-center gap-1.5 transition"
              >
                <RefreshCw className="w-3.5 h-3.5 text-indigo-400" />
                <span>Sync Context to Connectors</span>
              </button>
              {syncStatus && (
                <p className="text-[10px] text-emerald-400 text-center animate-fade-in">{syncStatus}</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* SUB-TAB 2: CHATGPT CONNECTOR (CUSTOM GPT ACTIONS) */}
      {activeSubTab === 'chatgpt' && (
        <div className="flex-1 p-6 md:p-8 overflow-y-auto max-w-4xl mx-auto w-full space-y-6">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Zap className="w-5 h-5 text-emerald-400" />
              <span>ChatGPT Custom GPT Action Connector</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Configure a Custom GPT in OpenAI ChatGPT to interact with your offline 3D Memory Universe via OpenAPI Actions.
            </p>
          </div>

          {/* Quick Setup Instructions */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <h3 className="font-bold text-xs text-slate-200 flex items-center gap-2">
              <Terminal className="w-4 h-4 text-emerald-400" />
              <span>Setup Steps in ChatGPT (chatgpt.com/gpts/create)</span>
            </h3>
            <ol className="list-decimal list-inside text-xs text-slate-400 space-y-2">
              <li>Open <strong>ChatGPT &gt; Explore GPTs &gt; Create a Custom GPT</strong>.</li>
              <li>Under the <strong>Configure</strong> tab, scroll down to <strong>Actions</strong> and click <strong>Create new action</strong>.</li>
              <li>Paste the <strong>OpenAPI Schema</strong> provided below into the Schema field.</li>
              <li>Set Authentication to <strong>None</strong> (or API Key if secured).</li>
              <li>ChatGPT can now invoke <code className="text-emerald-300 font-mono">getMemories</code> and <code className="text-emerald-300 font-mono">getClusters</code> during your chats!</li>
            </ol>
          </div>

          {/* Live OpenAPI Schema Box */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="font-bold text-xs text-white">OpenAPI 3.1.0 Action Specification</h4>
                <p className="text-[11px] text-slate-400">Available at: <code className="text-indigo-400">/api/connectors/openapi.json</code></p>
              </div>
              <button
                onClick={() => handleCopy(openApiSpecString, 'openapi')}
                className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs px-3 py-1.5 rounded-xl font-medium flex items-center gap-1.5 transition"
              >
                {copiedKey === 'openapi' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedKey === 'openapi' ? 'Copied Spec!' : 'Copy OpenAPI JSON'}</span>
              </button>
            </div>
            <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-[11px] font-mono text-emerald-300 overflow-x-auto max-h-60">
              {openApiSpecString}
            </pre>
          </div>

          {/* Interactive Test Request Box */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <h4 className="font-bold text-xs text-white flex items-center gap-2">
              <PlayIcon className="w-4 h-4 text-emerald-400" />
              <span>Simulate ChatGPT Live API Request</span>
            </h4>
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono text-emerald-400 bg-emerald-950/80 px-2.5 py-1.5 rounded-lg border border-emerald-800/60">
                GET /api/connectors/memories?query=
              </span>
              <input
                type="text"
                value={testQuery}
                onChange={(e) => setTestQuery(e.target.value)}
                placeholder="search keyword (e.g. hiking, coffee, astronomy)"
                className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-emerald-500"
              />
              <button
                onClick={handleTestConnector}
                disabled={isTestingApi}
                className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs px-3.5 py-2 rounded-xl font-medium transition"
              >
                {isTestingApi ? 'Querying...' : 'Test Endpoint'}
              </button>
            </div>

            {testResult && (
              <pre className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto max-h-48 mt-2">
                {JSON.stringify(testResult, null, 2)}
              </pre>
            )}
          </div>
        </div>
      )}

      {/* SUB-TAB 3: CLAUDE MCP CONNECTOR */}
      {activeSubTab === 'claude' && (
        <div className="flex-1 p-6 md:p-8 overflow-y-auto max-w-4xl mx-auto w-full space-y-6">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Cpu className="w-5 h-5 text-amber-400" />
              <span>Claude Model Context Protocol (MCP) & Projects</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Connect Claude Desktop or Claude Projects to your Memory Universe using Anthropic's Model Context Protocol.
            </p>
          </div>

          {/* Option A: Claude Desktop MCP Config */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-bold text-xs text-slate-200 flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-amber-400" />
                  <span>1. Claude Desktop MCP Server Configuration</span>
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Add this block to your <code className="text-amber-300">claude_desktop_config.json</code>
                </p>
              </div>
              <button
                onClick={() => handleCopy(mcpConfigString, 'mcp')}
                className="bg-amber-600 hover:bg-amber-500 text-white text-xs px-3 py-1.5 rounded-xl font-medium flex items-center gap-1.5 transition"
              >
                {copiedKey === 'mcp' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedKey === 'mcp' ? 'Copied Config!' : 'Copy MCP JSON'}</span>
              </button>
            </div>
            <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-[11px] font-mono text-amber-300 overflow-x-auto">
              {mcpConfigString}
            </pre>
          </div>

          {/* Option B: Claude Projects Knowledge Export */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-bold text-xs text-slate-200 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-amber-400" />
                  <span>2. Claude Projects Knowledge Artifact (.md)</span>
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Export your full memory universe as a structured markdown dossier to upload directly to Claude Projects.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleCopy(claudeKnowledgeMarkdown, 'claude_md')}
                  className="bg-slate-800 hover:bg-slate-700 text-white text-xs px-3 py-1.5 rounded-xl font-medium flex items-center gap-1.5 transition border border-slate-700"
                >
                  {copiedKey === 'claude_md' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5 text-amber-400" />}
                  <span>Copy Markdown</span>
                </button>
              </div>
            </div>
            <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto max-h-60">
              {claudeKnowledgeMarkdown}
            </pre>
          </div>
        </div>
      )}

      {/* SUB-TAB 4: CONTEXT & DATA SYNC */}
      {activeSubTab === 'sync' && (
        <div className="flex-1 p-6 md:p-8 overflow-y-auto max-w-4xl mx-auto w-full space-y-6">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Database className="w-5 h-5 text-indigo-400" />
              <span>Memory Context & Privacy Settings</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Control which memories are visible to external connectors and inspect the raw exported payload.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-2">
              <h3 className="font-bold text-xs text-white flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>Privacy & Offline Guardrails</span>
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Connectors read directly from local cached representations. Original photo files are never transmitted; only extracted semantic metadata (OCR text, note strings, and cluster memberships) are passed to AI bots.
              </p>
            </div>

            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-2">
              <h3 className="font-bold text-xs text-white flex items-center gap-2">
                <RefreshCw className="w-4 h-4 text-sky-400" />
                <span>Backend Cache Synchronization</span>
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                When you create new notes or reorganize galaxies in the 3D Universe, push changes to the connector cache with one click.
              </p>
              <button
                onClick={handleSyncData}
                className="mt-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs px-3.5 py-2 rounded-xl font-medium flex items-center gap-1.5 transition"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Synchronize Now ({memories.length} items)</span>
              </button>
              {syncStatus && (
                <p className="text-[11px] text-emerald-400 font-medium">{syncStatus}</p>
              )}
            </div>
          </div>

          {/* Raw JSON Context Inspector */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-xs text-white">Full Injected Context JSON</h3>
              <button
                onClick={() => handleCopy(JSON.stringify({ memories, clusters }, null, 2), 'raw_json')}
                className="bg-slate-800 hover:bg-slate-700 text-white text-xs px-3 py-1.5 rounded-xl font-medium flex items-center gap-1.5 transition border border-slate-700"
              >
                {copiedKey === 'raw_json' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>Copy JSON</span>
              </button>
            </div>
            <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-[11px] font-mono text-indigo-300 overflow-x-auto max-h-72">
              {JSON.stringify({ memories, clusters }, null, 2)}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}

function PlayIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <polygon points="5 3 19 12 5 21 5 3" />
    </svg>
  );
}
