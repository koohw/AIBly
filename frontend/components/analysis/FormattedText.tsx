import React from 'react';

interface FormattedTextProps {
  text: string;
}

export const FormattedTexts : React.FC<FormattedTextProps> = ({ text }) => {
  // 텍스트를 개행 문자로 분리
  const lines = text.split('\n');
  let boldCount = 0;

  return (
    <div className="text-sm">
      {lines.map((line, index) => {
        // 빈 줄이면 <br />
        if (!line) {
          return <br key={`empty-${index}`} />;
        }

        // ◆로 시작하는 줄은 볼드 처리
        if (line.startsWith('◆')) {
          boldCount++;
          const elements: React.ReactNode[] = [];

          // 두 번째 ◆부터는 해당 줄 전에 두 번 줄바꿈
          if (boldCount >= 2) {
            elements.push(
              <br key={`before-br1-${index}`} />,
              <br key={`before-br2-${index}`} />
            );
          }

          elements.push(
            <p key={`bold-${index}`} className="font-semibold mb-1">
              {line}
            </p>
          );

          return elements;
        }

        // 일반 텍스트
        return (
          <p key={`normal-${index}`} className="mb-1">
            {line}
          </p>
        );
      })}
    </div>
  );
};
