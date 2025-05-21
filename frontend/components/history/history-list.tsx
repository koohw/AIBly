"use client"

import { ChevronRight } from "lucide-react"
import type { AnalysisHistory } from "@/types/analysis"
import Accident from "@/public/image/accident.jpg"

interface HistoryListProps {
  items: AnalysisHistory[]
  onItemClick: (id: string) => void
}

export default function HistoryList({ items, onItemClick }: HistoryListProps) {
  return (
    <div className="app-card overflow-hidden">
      {items.map((item) => (
        <div
          key={item.id}
          className="p-4 border-b border-border flex items-center cursor-pointer"
          onClick={() => onItemClick(item.id)}
        >
          <div className="w-16 h-16 bg-muted rounded-md mr-3 flex-shrink-0 overflow-hidden relative">
            <img
              src={Accident.src}
              alt="사고이미지"
              className="w-full h-full object-cover"
            />
            {item.status === "processing" && (
              <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>
          <div className="flex-1">
            <h3 className="font-medium">{item.title}</h3>
            <p className="text-xs text-muted-foreground">{item.date}</p>
            <div className="flex mt-1">
              {item.faultA && (
                <span className="text-xs bg-appblue/20 text-appblue px-2 py-0.5 rounded mr-1">
                  과실비율 {item.faultA} : {item.faultB}
                </span>
              )}
              {item.tags.map((tag, index) => (
                <span
                  key={index}
                  className={`text-xs px-2 py-0.5 rounded mr-1 ${
                    tag === "분석중"
                      ? "bg-yellow-500/20 text-yellow-700 dark:text-yellow-500"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>
          <ChevronRight size={20} className="text-muted-foreground" />
        </div>
      ))}
    </div>
  )
}
