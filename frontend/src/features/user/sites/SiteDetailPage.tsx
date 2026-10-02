import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, MapPin, User, Calendar, Save, X } from 'lucide-react';
import { FormActions } from '@/components/forms/form-layout';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { PageLoadingOverlay } from '@/components/feedback/loading';
import { paths } from '@/routes/paths';
import { ActionButton } from '@/components/actions';

interface SiteData {
  siteId: string;
  name: string;
  postalCode: string;
  prefecture: string;
  city: string;
  address: string;
  building?: string;
  personInChargeId: string;
  personInChargeName: string;
  personInChargeEmail: string;
  joinedProjects: Array<{
    id: string;
    name: string;
    status: string;
    joinedDate: string;
  }>;
  createdAt: string;
}

interface CompanyUser {
  id: string;
  name: string;
  email: string;
  role: 'master' | 'regular';
}

/** Renders a site's details and available management actions. */
const SiteDetail = () => {
  const { siteId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const [site, setSite] = useState<SiteData | null>(null);
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [editedData, setEditedData] = useState<Partial<SiteData>>({});

  // Mock company users
  // 
  // ========== API Integration Stub for Backend ==========
  // async function fetchCompanyUsers(companyId: string) {
  //   // Endpoint: GET /api/companies/:companyId/users
  //   // Authentication: Required (Bearer token)
  //   // Headers: { 'Authorization': 'Bearer <token>', 'Content-Type': 'application/json' }
  //   const response = await fetch(`/api/companies/${companyId}/users`, {
  //     method: 'GET',
  //     headers: {
  //       'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
  //       'Content-Type': 'application/json'
  //     }
  //   });
  //   if (!response.ok) throw new Error('Failed to fetch company users');
  //   const data = await response.json();
  //   // Expected response: Array<{ id: string, name: string, email: string, role: 'master' | '一般' }>
  //   return data;
  // }
  // ======================================================
  const companyUsers: CompanyUser[] = [
    { id: '1', name: user?.name || 'デモ社員', email: user?.email || 'demo@example.com', role: 'master' },
    { id: '2', name: '山田太郎', email: 'yamada@example.com', role: 'regular' },
    { id: '3', name: '佐藤花子', email: 'sato@example.com', role: 'regular' },
    { id: '4', name: '鈴木一郎', email: 'suzuki@example.com', role: 'regular' }
  ];

  const fetchSiteDetail = useCallback(async () => {
    setLoading(true);
    try {
      // Mock data - would come from API
      // 
      // ========== API Integration Stub for Backend ==========
      // const response = await fetch(`/api/sites/${siteId}`, {
      //   method: 'GET',
      //   headers: {
      //     'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
      //     'Content-Type': 'application/json'
      //   }
      // });
      // if (!response.ok) throw new Error('Failed to fetch site');
      // const data = await response.json();
      // Expected response structure:
      // {
      //   siteId: string,
      //   name: string,
      //   postalCode: string,
      //   prefecture: string,
      //   city: string,
      //   address: string,
      //   building?: string,
      //   personInChargeId: string,
      //   personInChargeName: string,
      //   personInChargeEmail: string,
      //   joinedProjects: Array<{ id: string, name: string, status: string, joinedDate: string }>,
      //   createdAt: string (ISO 8601)
      // }
      // setSite(data);
      // return;
      // ======================================================
      await new Promise(resolve => setTimeout(resolve, 500));
      
      const mockSite: SiteData = {
        siteId: siteId || 'SITEaB3kM9pQ7xL2vN8s',
        name: '東京本社',
        postalCode: '100-0001',
        prefecture: '東京都',
        city: '千代田区',
        address: '丸の内1-1',
        building: '〇〇ビル 5階',
        personInChargeId: user?.id || '1',
        personInChargeName: user?.username || 'デモ社員',
        personInChargeEmail: user?.email || 'demo@example.com',
        joinedProjects: [
          { id: 'PROJ001', name: '環境モニタリングプロジェクトA', status: 'active', joinedDate: '2024-01-15' },
          { id: 'PROJ002', name: 'エネルギー効率化プロジェクト', status: 'active', joinedDate: '2024-02-01' }
        ],
        createdAt: '2024-01-10'
      };

      setSite(mockSite);
      setEditedData(mockSite);
    } catch (error) {
      toast({
        title: 'エラー',
        description: '拠点情報の取得に失敗しました',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
    }, [siteId, toast, user?.email, user?.id, user?.username]);

  useEffect(() => {
    fetchSiteDetail();
  }, [fetchSiteDetail]);

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancel = () => {
    setEditedData(site || {});
    setIsEditing(false);
  };

  const handleSave = async () => {
    try {
      // Mock API call
      // 
      // ========== API Integration Stub for Backend ==========
      // const response = await fetch(`/api/sites/${site?.siteId}`, {
      //   method: 'PATCH',
      //   headers: {
      //     'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
      //     'Content-Type': 'application/json'
      //   },
      //   body: JSON.stringify(editedData)
      // });
      // if (!response.ok) throw new Error('Failed to update site');
      // const updatedSite = await response.json();
      // setSite(updatedSite);
      // ======================================================
      await new Promise(resolve => setTimeout(resolve, 500));

      // Update person in charge name if changed
      if (editedData.personInChargeId !== site?.personInChargeId) {
        const newPerson = companyUsers.find(u => u.id === editedData.personInChargeId);
        if (newPerson) {
          editedData.personInChargeName = newPerson.name;
          editedData.personInChargeEmail = newPerson.email;
        }
      }

      setSite(editedData as SiteData);
      setIsEditing(false);

      toast({
        title: '更新完了',
        description: '拠点情報を更新しました'
      });
    } catch (error) {
      toast({
        title: 'エラー',
        description: '更新に失敗しました',
        variant: 'destructive'
      });
    }
  };

  if (loading) {
    // Skeleton halaman dinonaktifkan sementara sampai layout final halaman ini dipetakan.
    return <PageLoadingOverlay message="拠点情報を読み込んでいます" />;
  }

  if (!site) {
    return (
      <div className="container mx-auto p-6">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-foreground">拠点が見つかりません</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      <PageBackButton
        onClick={() => navigate('/profile')}
        label='プロファイルに戻る'
        className='mb-4 rounded-full'
      />
      <Button 
        variant="ghost" 
        onClick={() => navigate('/profile')}
        className="hidden"
      >
        <ArrowLeft className="h-4 w-4 mr-2" />
        プロフィールに戻る
      </Button>

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-form-title font-bold flex items-center gap-2">
            <MapPin className="h-8 w-8 text-primary" />
            {site.name}
          </h1>
          <p className="text-foreground mt-2">
            {site.prefecture}{site.city}{site.address}
          </p>
        </div>
        {user?.role === 'master' && !isEditing && (
          <ActionButton kind="edit" onClick={handleEdit}>
            編集
          </ActionButton>
        )}
      </div>

      <Tabs defaultValue="info" className="space-y-4">
        <TabsList>
          <TabsTrigger value="info">基本情報</TabsTrigger>
          <TabsTrigger value="projects">参加プロジェクト</TabsTrigger>
        </TabsList>

        <TabsContent value="info">
          <Card>
            <CardHeader>
              <CardTitle>拠点情報</CardTitle>
              <CardDescription>この拠点の詳細情報</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {isEditing ? (
                <>
                  <div className="space-y-2">
                    <Label>拠点名</Label>
                    <Input
                      value={editedData.name || ''}
                      onChange={(e) => setEditedData({ ...editedData, name: e.target.value })}
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>郵便番号</Label>
                      <Input
                        value={editedData.postalCode || ''}
                        onChange={(e) => setEditedData({ ...editedData, postalCode: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>都道府県</Label>
                      <Input
                        value={editedData.prefecture || ''}
                        onChange={(e) => setEditedData({ ...editedData, prefecture: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>市区町村</Label>
                      <Input
                        value={editedData.city || ''}
                        onChange={(e) => setEditedData({ ...editedData, city: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>番地</Label>
                      <Input
                        value={editedData.address || ''}
                        onChange={(e) => setEditedData({ ...editedData, address: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label>建物名・部屋番号</Label>
                    <Input
                      value={editedData.building || ''}
                      onChange={(e) => setEditedData({ ...editedData, building: e.target.value })}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label>責任者</Label>
                    <Select
                      value={editedData.personInChargeId || ''}
                      onValueChange={(value) => setEditedData({ ...editedData, personInChargeId: value })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="bg-background z-50">
                        {companyUsers.map((user) => (
                          <SelectItem key={user.id} value={user.id}>
                            {user.name} ({user.role === 'master' ? 'master' : 'user'}) - {user.email}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <FormActions
                    className="pt-4"
                    actions={[
                      {
                        id: 'cancel',
                        label: (
                          <>
                            <X className="h-4 w-4" />
                            キャンセル
                          </>
                        ),
                        variant: 'outline',
                        onClick: handleCancel,
                      },
                      {
                        id: 'save',
                        label: (
                          <>
                            <Save className="h-4 w-4" />
                            保存
                          </>
                        ),
                        onClick: handleSave,
                      },
                    ]}
                  />
                </>
              ) : (
                <>
                  <div className="grid grid-cols-2 gap-6">
                    <div>
                      <p className="text-sm text-foreground mb-1">郵便番号</p>
                      <p className="font-medium">{site.postalCode}</p>
                    </div>
                    <div>
                      <p className="text-sm text-foreground mb-1">都道府県</p>
                      <p className="font-medium">{site.prefecture}</p>
                    </div>
                    <div>
                      <p className="text-sm text-foreground mb-1">市区町村</p>
                      <p className="font-medium">{site.city}</p>
                    </div>
                    <div>
                      <p className="text-sm text-foreground mb-1">番地</p>
                      <p className="font-medium">{site.address}</p>
                    </div>
                    {site.building && (
                      <div className="col-span-2">
                        <p className="text-sm text-foreground mb-1">建物名・部屋番号</p>
                        <p className="font-medium">{site.building}</p>
                      </div>
                    )}
                  </div>

                  <div className="border-t pt-6">
                    <div className="flex items-center gap-2 mb-4">
                      <User className="h-5 w-5 text-primary" />
                      <h3 className="font-semibold">担当者情報</h3>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-foreground mb-1">氏名</p>
                        <p className="font-medium">{site.personInChargeName}</p>
                      </div>
                      <div>
                        <p className="text-sm text-foreground mb-1">メールアドレス</p>
                        <p className="font-medium">{site.personInChargeEmail}</p>
                      </div>
                    </div>
                  </div>

                  <div className="border-t pt-6">
                    <div className="flex items-center gap-2">
                      <Calendar className="h-5 w-5 text-primary" />
                      <div>
                        <p className="text-sm text-foreground">登録日</p>
                        <p className="font-medium">{site.createdAt}</p>
                      </div>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="projects">
          <Card>
            <CardHeader>
              <CardTitle>参加プロジェクト</CardTitle>
              <CardDescription>この拠点が参加しているプロジェクト一覧</CardDescription>
            </CardHeader>
            <CardContent>
              {site.joinedProjects.length === 0 ? (
                <p className="text-center text-foreground py-8">
                  まだプロジェクトに参加していません
                </p>
              ) : (
                <div className="space-y-4">
                  {site.joinedProjects.map((project) => (
                    <div
                      key={project.id}
                      className="flex items-center justify-between p-4 border rounded-lg hover:bg-accent/50 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate(
                          paths.siteProjectDetail(
                            site.siteId,
                            project.id,
                          ),
                        )
                      }
                    >
                      <div>
                        <p className="font-medium">{project.name}</p>
                        <p className="text-sm text-foreground">
                          参加日: {project.joinedDate}
                        </p>
                      </div>
                      <Badge variant={project.status === 'active' ? 'default' : 'secondary'}>
                        {project.status === 'active' ? '稼働中' : '停止中'}
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default SiteDetail;
