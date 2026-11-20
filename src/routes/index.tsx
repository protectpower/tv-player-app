import { createFileRoute } from "@tanstack/react-router";
import { useEffect } from "react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "cPlayer TV" },
      { name: "description", content: "cPlayer TV: canais, filmes e séries com navegação pelo controle remoto." },
      { property: "og:title", content: "cPlayer TV" },
      { property: "og:description", content: "Canais, filmes e séries na sua TV." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Index,
});

function Index() {
  useEffect(() => {
    window.location.replace("/tv.html");
  }, []);
  return (
    <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
      <a href="/tv.html">Abrindo cPlayer TV…</a>
    </div>
  );
}
