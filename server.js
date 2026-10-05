// backend-proxy/server.js
const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');
require('dotenv').config();

const app = express();
app.use(express.json());
app.use(cors());

const PORT = process.env.PORT || 3000;
const API_KEY = process.env.AI_API_KEY || "YOUR_OPENAI_OR_OPENROUTER_API_KEY";
const TARGET_URL = process.env.AI_API_BASE_URL || "https://api.openai.com/v1/chat/completions";

app.post('/v1/chat/completions', async (req, res) => {
    try {
        const { messages, model = "gpt-4o-mini", temperature = 0.7, stream = true } = req.body;

        if (!messages || !Array.isArray(messages)) {
            return res.status(400).json({ error: { message: "Format request tidak valid." } });
        }

        const systemPrompt = {
            role: "system",
            content: "ZYY AI adalah asisten AI pribadi yang cerdas, responsif, memahami bahasa Indonesia secara natural, mempertahankan konteks percakapan, membantu coding, analisis, penulisan, pembelajaran, matematika, dan berbagai tugas pengguna. Jawaban harus relevan dengan instruksi pengguna dan tidak mengarang kemampuan yang tidak tersedia."
        };

        const apiPayload = {
            model,
            messages: [systemPrompt, ...messages],
            temperature,
            stream
        };

        const response = await fetch(TARGET_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${API_KEY}`
            },
            body: JSON.stringify(apiPayload)
        });

        if (!response.ok) {
            const errData = await response.text();
            return res.status(response.status).send(errData);
        }

        if (stream) {
            res.setHeader('Content-Type', 'text/event-stream');
            res.setHeader('Cache-Control', 'no-cache');
            res.setHeader('Connection', 'keep-alive');
            response.body.pipe(res);
        } else {
            const data = await response.json();
            res.json(data);
        }
    } catch (err) {
        res.status(500).json({ error: { message: "Koneksi ke proxy server gagal: " + err.message } });
    }
});

app.listen(PORT, () => {
    console.log(`Proxy ZYY AI aktif di port ${PORT}`);
});
